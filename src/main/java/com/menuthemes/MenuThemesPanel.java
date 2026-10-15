package com.menuthemes;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.ColorJButton;
import net.runelite.client.ui.components.TitleCaseListCellRenderer;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.ui.components.colorpicker.RuneliteColorPicker;
import net.runelite.client.util.ColorUtil;

/**
 * Styled to match the native per-plugin config panel (ConfigPanel): relies on the same inherited
 * {@code PluginPanel} padding/layout, {@link ColorJButton} colour swatches, {@link TitleCaseListCellRenderer}
 * for enum combos, and {@link CollapsibleSection}s that mirror ConfigPanel's section chrome.
 */
class MenuThemesPanel extends PluginPanel
{
	private final Client client;
	private final ThemeManager themeManager;
	private final MenuThemesConfig config;
	private final ConfigManager configManager;
	private final ColorPickerManager colorPickerManager;

	private final ThemePreview preview;
	private final JComboBox<Theme> themeCombo = new JComboBox<>();
	private final JButton saveButton = new JButton("Save");
	private final JButton revertButton = new JButton("Revert");

	// Theme-bound controls; re-synced whenever the active/working theme changes.
	private final List<Runnable> themeControlRefreshers = new ArrayList<>();
	private JSpinner fontSizeSpinner;

	private final JPanel rulesPanel = new JPanel();
	private final List<Rule> rules = new ArrayList<>();

	private boolean refreshing;

	@Inject
	MenuThemesPanel(Client client, ThemeManager themeManager, MenuThemesConfig config,
		ConfigManager configManager, ColorPickerManager colorPickerManager)
	{
		this.client = client;
		this.themeManager = themeManager;
		this.config = config;
		this.configManager = configManager;
		this.colorPickerManager = colorPickerManager;

		preview = new ThemePreview(client, themeManager, config);

		add(preview);
		add(buildThemeRow());
		add(buildColoursSection());
		add(buildStyleSection());
		add(buildTextSection());
		add(buildHighlightsSection());
		add(buildTargetsSection());
		add(buildLevelsSection());
		add(buildAdvancedSection());

		refresh();
	}

	// --- Theme row: combo + save/revert/more ---

	private JPanel buildThemeRow()
	{
		JPanel container = new JPanel();
		container.setLayout(new DynamicGridLayout(0, 1, 0, 4));

		themeCombo.setRenderer(new DefaultListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
			{
				String text = value == null ? "" : themeLabel((Theme) value);
				return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
			}
		});
		themeCombo.addActionListener(e ->
		{
			if (refreshing)
			{
				return;
			}
			Theme selected = (Theme) themeCombo.getSelectedItem();
			if (selected != null && !selected.id.equals(themeManager.activeId()))
			{
				themeManager.applyTheme(selected.id);
				refresh();
			}
		});
		container.add(themeCombo);

		JPanel buttons = new JPanel(new GridLayout(2, 2, 4, 4));
		saveButton.addActionListener(e ->
		{
			themeManager.save();
			refresh();
		});
		buttons.add(compact(saveButton));

		JButton saveAsButton = new JButton("Save as…");
		saveAsButton.addActionListener(e -> promptSaveAsNew());
		buttons.add(compact(saveAsButton));

		revertButton.addActionListener(e ->
		{
			themeManager.revert();
			refresh();
		});
		buttons.add(compact(revertButton));

		buttons.add(compact(buildMoreButton()));
		container.add(buttons);

		return container;
	}

	private static JButton compact(JButton button)
	{
		button.setFont(FontManager.getRunescapeSmallFont());
		button.setMargin(new Insets(2, 2, 2, 2));
		return button;
	}

	private JButton buildMoreButton()
	{
		JButton button = new JButton("More ▾");
		button.setToolTipText("More theme actions");

		JPopupMenu menu = new JPopupMenu();
		JMenuItem renameItem = new JMenuItem("Rename");
		renameItem.addActionListener(e -> promptRename());
		JMenuItem duplicateItem = new JMenuItem("Duplicate");
		duplicateItem.addActionListener(e ->
		{
			themeManager.duplicate();
			refresh();
		});
		JMenuItem deleteItem = new JMenuItem("Delete");
		deleteItem.addActionListener(e -> promptDelete());
		JMenuItem exportItem = new JMenuItem("Export");
		exportItem.addActionListener(e -> exportActive());
		JMenuItem importItem = new JMenuItem("Import");
		importItem.addActionListener(e -> promptImport());

		menu.add(renameItem);
		menu.add(duplicateItem);
		menu.add(deleteItem);
		menu.addSeparator();
		menu.add(exportItem);
		menu.add(importItem);

		menu.addPopupMenuListener(new PopupMenuListener()
		{
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e)
			{
				boolean userTheme = !themeManager.active().builtIn;
				renameItem.setEnabled(userTheme);
				deleteItem.setEnabled(userTheme);
			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
			{
			}

			@Override
			public void popupMenuCanceled(PopupMenuEvent e)
			{
			}
		});

		button.addActionListener(e -> menu.show(button, 0, button.getHeight()));
		return button;
	}

	private static String themeLabel(Theme theme)
	{
		return theme.builtIn ? "🔒 " + theme.name : theme.name;
	}

	// --- Theme-bound sections ---

	private CollapsibleSection buildColoursSection()
	{
		CollapsibleSection section = new CollapsibleSection("Colours", "The active theme's colours", false);
		section.addRow(themeColourRow("Background", t -> t.bg, Theme::withBg));
		section.addRow(themeColourRow("Border", t -> t.border, Theme::withBorder));
		section.addRow(themeColourRow("Header", t -> t.header, Theme::withHeader));
		section.addRow(themeColourRow("Header text", t -> t.headerText, Theme::withHeaderText));
		section.addRow(themeColourRow("Option text", t -> t.text, Theme::withText));
		section.addRow(themeColourRow("Hover", t -> t.hover, Theme::withHover));
		section.addRow(themeColourRow("Accent", t -> t.accent, Theme::withAccent));
		return section;
	}

	private CollapsibleSection buildStyleSection()
	{
		CollapsibleSection section = new CollapsibleSection("Style", "The active theme's shape and hover style", false);

		JSpinner radiusSpinner = themeIntSpinner(t -> t.radius, Theme::withRadius, 0, 8);
		section.addRow(row("Corner radius", radiusSpinner));

		JSpinner opacitySpinner = themeIntSpinner(t -> t.opacity, Theme::withOpacity, 40, 100);
		section.addRow(row("Opacity", opacitySpinner));

		JComboBox<HoverStyle> hoverCombo = new JComboBox<>(HoverStyle.values());
		hoverCombo.setRenderer(new TitleCaseListCellRenderer());
		hoverCombo.addActionListener(e ->
		{
			if (!refreshing)
			{
				apply(t -> t.withHoverStyle((HoverStyle) hoverCombo.getSelectedItem()));
			}
		});
		themeControlRefreshers.add(() -> hoverCombo.setSelectedItem(themeManager.effectiveTheme().hoverStyle));
		section.addRow(row("Hover style", hoverCombo));

		section.addRow(row("Border", themeCheckBox(t -> t.showBorder, Theme::withShowBorder)));
		section.addRow(row("Drop shadow", themeCheckBox(t -> t.showShadow, Theme::withShowShadow)));
		section.addRow(row("Header", themeCheckBox(t -> t.showHeader, Theme::withShowHeader)));

		return section;
	}

	private CollapsibleSection buildTextSection()
	{
		CollapsibleSection section = new CollapsibleSection("Text", "The active theme's font", false);

		JComboBox<FontChoice> fontCombo = new JComboBox<>(FontChoice.values());
		fontCombo.setRenderer(new TitleCaseListCellRenderer());
		fontCombo.addActionListener(e ->
		{
			FontChoice selected = (FontChoice) fontCombo.getSelectedItem();
			fontSizeSpinner.setEnabled(selected == FontChoice.SANS || selected == FontChoice.MONO);
			if (!refreshing)
			{
				apply(t -> t.withFont(selected));
			}
		});
		themeControlRefreshers.add(() -> fontCombo.setSelectedItem(themeManager.effectiveTheme().font));
		section.addRow(row("Font", fontCombo));

		fontSizeSpinner = themeIntSpinner(t -> t.fontSize, Theme::withFontSize, 11, 13);
		section.addRow(row("Font size", "Applies to the Sans and Mono fonts only", fontSizeSpinner));

		section.addRow(row("Text shadow", themeCheckBox(t -> t.textShadow, Theme::withTextShadow)));

		return section;
	}

	private JSpinner themeIntSpinner(Function<Theme, Integer> getter, BiFunction<Theme, Integer, Theme> setter,
		int min, int max)
	{
		JSpinner spinner = new JSpinner(new SpinnerNumberModel(min, min, max, 1));
		spinnerFieldWidth(spinner);
		spinner.addChangeListener(e ->
		{
			if (!refreshing)
			{
				apply(t -> setter.apply(t, (Integer) spinner.getValue()));
			}
		});
		themeControlRefreshers.add(() -> spinner.setValue(getter.apply(themeManager.effectiveTheme())));
		return spinner;
	}

	private JCheckBox themeCheckBox(Function<Theme, Boolean> getter, BiFunction<Theme, Boolean, Theme> setter)
	{
		JCheckBox checkbox = new JCheckBox();
		checkbox.addActionListener(e ->
		{
			if (!refreshing)
			{
				apply(t -> setter.apply(t, checkbox.isSelected()));
			}
		});
		themeControlRefreshers.add(() -> checkbox.setSelected(getter.apply(themeManager.effectiveTheme())));
		return checkbox;
	}

	private JPanel themeColourRow(String label, Function<Theme, Color> getter,
		BiFunction<Theme, Color, Theme> setter)
	{
		ColorJButton button = colourButton(getter.apply(themeManager.effectiveTheme()), label, c -> apply(t -> setter.apply(t, c)));
		themeControlRefreshers.add(() -> setColourButton(button, getter.apply(themeManager.effectiveTheme())));
		return row(label, button);
	}

	/**
	 * Applies a single edit to the working copy and refreshes every dependent view.
	 */
	private void apply(Function<Theme, Theme> mutator)
	{
		themeManager.setWorking(mutator.apply(themeManager.effectiveTheme()));
		preview.repaint();
		themeCombo.repaint();
		saveButton.setEnabled(!themeManager.active().builtIn && themeManager.isEdited());
		revertButton.setEnabled(themeManager.isEdited());
	}

	// --- Global sections ---

	private CollapsibleSection buildHighlightsSection()
	{
		CollapsibleSection section = new CollapsibleSection("Action highlights", "Highlight specific menu options", false);

		JCheckBox enabled = new JCheckBox();
		enabled.setSelected(config.highlightsEnabled());
		enabled.addActionListener(e -> configManager.setConfiguration(MenuThemesConfig.GROUP, "highlightsEnabled", enabled.isSelected()));
		section.addRow(row("Enabled", enabled));

		JCheckBox bold = new JCheckBox();
		bold.setSelected(config.highlightsBold());
		bold.addActionListener(e -> configManager.setConfiguration(MenuThemesConfig.GROUP, "highlightsBold", bold.isSelected()));
		section.addRow(row("Bold", bold));

		rulesPanel.setLayout(new DynamicGridLayout(0, 1, 0, 4));
		rules.addAll(parseRules());
		for (Rule rule : rules)
		{
			rulesPanel.add(ruleRow(rule, false));
		}
		section.addRow(rulesPanel);

		JButton addRule = new JButton("+ Add rule");
		addRule.addActionListener(e -> addNewRule());
		section.addRow(addRule);

		return section;
	}

	private CollapsibleSection buildTargetsSection()
	{
		CollapsibleSection section = new CollapsibleSection("Targets", "Colour menu targets by entity type", false);

		JComboBox<TargetMode> modeCombo = new JComboBox<>(TargetMode.values());
		modeCombo.setRenderer(new TitleCaseListCellRenderer());
		modeCombo.setSelectedItem(config.targetMode());

		ColorJButton npc = colourButton(config.npcColour(), "NPC colour", globalColourWriter("npcColour"));
		ColorJButton player = colourButton(config.playerColour(), "Player colour", globalColourWriter("playerColour"));
		ColorJButton item = colourButton(config.itemColour(), "Item colour", globalColourWriter("itemColour"));
		ColorJButton object = colourButton(config.objectColour(), "Object colour", globalColourWriter("objectColour"));
		ColorJButton other = colourButton(config.otherColour(), "Other colour", globalColourWriter("otherColour"));

		Runnable updateEnabled = () ->
		{
			boolean custom = modeCombo.getSelectedItem() == TargetMode.CUSTOM;
			npc.setEnabled(custom);
			player.setEnabled(custom);
			item.setEnabled(custom);
			object.setEnabled(custom);
			other.setEnabled(custom);
		};
		updateEnabled.run();
		modeCombo.addActionListener(e ->
		{
			configManager.setConfiguration(MenuThemesConfig.GROUP, "targetMode", modeCombo.getSelectedItem());
			updateEnabled.run();
		});

		section.addRow(row("Mode", modeCombo));
		section.addRow(row("NPC", npc));
		section.addRow(row("Player", player));
		section.addRow(row("Item", item));
		section.addRow(row("Object", object));
		section.addRow(row("Other", other));
		return section;
	}

	private CollapsibleSection buildLevelsSection()
	{
		CollapsibleSection section = new CollapsibleSection("Combat levels", "Colour the combat level shown in targets", false);

		JComboBox<CombatLevelMode> modeCombo = new JComboBox<>(CombatLevelMode.values());
		modeCombo.setRenderer(new TitleCaseListCellRenderer());
		modeCombo.setSelectedItem(config.combatLevelMode());

		ColorJButton lower = colourButton(config.lowerLevelColour(), "Lower level colour", globalColourWriter("lowerLevelColour"));
		ColorJButton same = colourButton(config.sameLevelColour(), "Same level colour", globalColourWriter("sameLevelColour"));
		ColorJButton higher = colourButton(config.higherLevelColour(), "Higher level colour", globalColourWriter("higherLevelColour"));

		Runnable updateEnabled = () ->
		{
			boolean custom = modeCombo.getSelectedItem() == CombatLevelMode.CUSTOM;
			lower.setEnabled(custom);
			same.setEnabled(custom);
			higher.setEnabled(custom);
		};
		updateEnabled.run();
		modeCombo.addActionListener(e ->
		{
			configManager.setConfiguration(MenuThemesConfig.GROUP, "combatLevelMode", modeCombo.getSelectedItem());
			updateEnabled.run();
		});

		section.addRow(row("Mode", modeCombo));
		section.addRow(row("Lower level", lower));
		section.addRow(row("Same level", same));
		section.addRow(row("Higher level", higher));
		return section;
	}

	private CollapsibleSection buildAdvancedSection()
	{
		CollapsibleSection section = new CollapsibleSection("Advanced", "Geometry and debugging", true);

		section.addRow(row("Header height", globalIntSpinner(config.headerHeight(), 0, 60, "headerHeight")));
		section.addRow(row("Row height", globalIntSpinner(config.rowHeight(), 5, 40, "rowHeight")));
		section.addRow(row("Text offset X", globalIntSpinner(config.textOffsetX(), 0, 20, "textOffsetX")));
		section.addRow(row("Baseline nudge", globalIntSpinner(config.baselineNudge(), -10, 10, "baselineNudge")));

		JCheckBox hitRows = new JCheckBox();
		hitRows.setSelected(config.showHitRows());
		hitRows.addActionListener(e -> configManager.setConfiguration(MenuThemesConfig.GROUP, "showHitRows", hitRows.isSelected()));
		section.addRow(row("Show hit rows", hitRows));

		return section;
	}

	private Consumer<Color> globalColourWriter(String key)
	{
		return c -> configManager.setConfiguration(MenuThemesConfig.GROUP, key, c);
	}

	private JSpinner globalIntSpinner(int value, int min, int max, String key)
	{
		JSpinner spinner = new JSpinner(new SpinnerNumberModel(value, min, max, 1));
		spinnerFieldWidth(spinner);
		spinner.addChangeListener(e -> configManager.setConfiguration(MenuThemesConfig.GROUP, key, spinner.getValue()));
		return spinner;
	}

	// --- Action highlight rules editor ---

	private static final class Rule
	{
		private String option;
		private Color colour;

		private Rule(String option, Color colour)
		{
			this.option = option;
			this.colour = colour;
		}
	}

	private List<Rule> parseRules()
	{
		List<Rule> parsed = new ArrayList<>();
		for (String line : config.highlightRules().split("\n"))
		{
			int eq = line.lastIndexOf('=');
			if (eq <= 0 || eq == line.length() - 1)
			{
				continue;
			}
			String option = line.substring(0, eq).trim();
			String hex = line.substring(eq + 1).trim();
			if (hex.startsWith("#"))
			{
				hex = hex.substring(1);
			}
			if (!hex.matches("[0-9a-fA-F]{6}"))
			{
				continue;
			}
			parsed.add(new Rule(option, Color.decode("#" + hex)));
		}
		return parsed;
	}

	/**
	 * Writes every current rule to config, skipping blank option names. Blank rows stay in the
	 * UI (and in {@link #rules}) while they're being edited - they just aren't persisted yet.
	 */
	private void persistRules()
	{
		StringBuilder sb = new StringBuilder();
		for (Rule rule : rules)
		{
			if (rule.option.trim().isEmpty())
			{
				continue;
			}
			if (sb.length() > 0)
			{
				sb.append('\n');
			}
			sb.append(rule.option.trim()).append('=').append(hex(rule.colour));
		}
		configManager.setConfiguration(MenuThemesConfig.GROUP, "highlightRules", sb.toString());
	}

	private void addNewRule()
	{
		Rule rule = new Rule("New rule", new Color(0xFF9F1C));
		rules.add(rule);
		rulesPanel.add(ruleRow(rule, true));
		rulesPanel.revalidate();
		rulesPanel.repaint();
	}

	private JPanel ruleRow(Rule rule, boolean focusField)
	{
		JPanel panel = new JPanel(new BorderLayout(4, 0));

		JTextField optionField = new JTextField(rule.option);
		optionField.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		Runnable commit = () ->
		{
			rule.option = optionField.getText().trim();
			persistRules();
		};
		optionField.addActionListener(e -> commit.run());
		optionField.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				commit.run();
			}
		});

		ColorJButton colourBtn = colourButton(rule.colour, "Rule colour", c ->
		{
			rule.colour = c;
			persistRules();
		});

		JButton remove = new JButton("✕");
		remove.setToolTipText("Remove rule");
		remove.setMargin(new Insets(2, 6, 2, 6));
		remove.addActionListener(e ->
		{
			rules.remove(rule);
			rulesPanel.remove(panel);
			rulesPanel.revalidate();
			rulesPanel.repaint();
			persistRules();
		});

		JPanel right = new JPanel(new BorderLayout(4, 0));
		right.add(colourBtn, BorderLayout.WEST);
		right.add(remove, BorderLayout.EAST);

		panel.add(optionField, BorderLayout.CENTER);
		panel.add(right, BorderLayout.EAST);

		if (focusField)
		{
			SwingUtilities.invokeLater(() ->
			{
				optionField.requestFocusInWindow();
				optionField.selectAll();
			});
		}

		return panel;
	}

	// --- Shared widgets ---

	private ColorJButton colourButton(Color initial, String title, Consumer<Color> onChange)
	{
		ColorJButton button = new ColorJButton(hex(initial), initial);
		button.setFocusable(false);
		button.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (!button.isEnabled())
				{
					return;
				}
				RuneliteColorPicker picker = colorPickerManager.create(button, button.getColor(), title, true);
				picker.setLocationRelativeTo(button);
				picker.setOnColorChange(c ->
				{
					setColourButton(button, c);
					onChange.accept(c);
				});
				picker.setVisible(true);
			}
		});
		return button;
	}

	private static void setColourButton(ColorJButton button, Color colour)
	{
		button.setColor(colour);
		button.setText(hex(colour));
	}

	private static String hex(Color colour)
	{
		return "#" + ColorUtil.colorToHexCode(colour).toUpperCase();
	}

	private static JPanel row(String label, JComponent control)
	{
		return row(label, null, control);
	}

	private static JPanel row(String label, String tooltip, JComponent control)
	{
		JPanel panel = new JPanel(new BorderLayout());
		JLabel text = new JLabel(label);
		if (tooltip != null)
		{
			text.setToolTipText(tooltip);
		}
		panel.add(text, BorderLayout.CENTER);
		panel.add(control, BorderLayout.EAST);
		return panel;
	}

	private static void spinnerFieldWidth(JSpinner spinner)
	{
		Component editor = spinner.getEditor();
		if (editor instanceof JSpinner.DefaultEditor)
		{
			((JSpinner.DefaultEditor) editor).getTextField().setColumns(4);
		}
	}

	// --- Refresh / theme actions ---

	private void refresh()
	{
		refreshing = true;
		try
		{
			themeCombo.removeAllItems();
			for (Theme theme : themeManager.builtIns())
			{
				themeCombo.addItem(theme);
			}
			for (Theme theme : themeManager.userThemes())
			{
				themeCombo.addItem(theme);
			}

			Theme active = themeManager.active();
			for (int i = 0; i < themeCombo.getItemCount(); i++)
			{
				if (themeCombo.getItemAt(i).id.equals(active.id))
				{
					themeCombo.setSelectedIndex(i);
					break;
				}
			}

			for (Runnable r : themeControlRefreshers)
			{
				r.run();
			}

			saveButton.setEnabled(!active.builtIn && themeManager.isEdited());
			revertButton.setEnabled(themeManager.isEdited());

			preview.repaint();
			themeCombo.repaint();
		}
		finally
		{
			refreshing = false;
		}
	}

	private void promptSaveAsNew()
	{
		String name = JOptionPane.showInputDialog(this, "Theme name:", "Save as new theme", JOptionPane.PLAIN_MESSAGE);
		if (name != null && !name.trim().isEmpty())
		{
			themeManager.saveAsNew(name.trim());
			refresh();
		}
	}

	private void promptRename()
	{
		String name = JOptionPane.showInputDialog(this, "New name:", themeManager.active().name);
		if (name != null && !name.trim().isEmpty())
		{
			themeManager.rename(name.trim());
			refresh();
		}
	}

	private void promptDelete()
	{
		Theme active = themeManager.active();
		int result = JOptionPane.showConfirmDialog(this, "Delete \"" + active.name + "\"?", "Delete theme",
			JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if (result == JOptionPane.YES_OPTION)
		{
			themeManager.delete(active.id);
			refresh();
		}
	}

	private void exportActive()
	{
		String payload = themeManager.export(themeManager.activeId());
		if (payload == null)
		{
			return;
		}
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(new StringSelection(payload), null);
	}

	private void promptImport()
	{
		String clipboardText = "";
		try
		{
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor))
			{
				clipboardText = (String) clipboard.getData(DataFlavor.stringFlavor);
			}
		}
		catch (UnsupportedFlavorException | IOException | IllegalStateException ignored)
		{
			// clipboard not readable; leave the field blank
		}

		String payload = (String) JOptionPane.showInputDialog(this, "Paste exported theme:", "Import theme",
			JOptionPane.PLAIN_MESSAGE, null, null, clipboardText);
		if (payload == null || payload.trim().isEmpty())
		{
			return;
		}

		try
		{
			themeManager.importTheme(payload);
			refresh();
		}
		catch (IllegalArgumentException e)
		{
			JOptionPane.showMessageDialog(this, "That doesn't look like a valid exported theme.",
				"Import failed", JOptionPane.ERROR_MESSAGE);
		}
	}
}
