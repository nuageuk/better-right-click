package com.betterrightclick;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
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
import javax.swing.Timer;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.ColorJButton;
import net.runelite.client.ui.components.TitleCaseListCellRenderer;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.ui.components.colorpicker.RuneliteColorPicker;
import net.runelite.client.util.ColorUtil;

/**
 * Sidebar settings panel, styled to match RuneLite's own plugin config panel.
 */
class BetterRightClickPanel extends PluginPanel
{
	private static final int ROW_GAP = 4;
	private static final int SPINNER_COLUMNS = 4;
	private static final int FIELD_PADDING = 4;
	private static final Insets COMPACT_INSETS = new Insets(2, 2, 2, 2);
	private static final Insets REMOVE_INSETS = new Insets(2, 6, 2, 6);
	private static final Color NEW_RULE_COLOUR = new Color(0xFF9F1C);
	private static final String NEW_RULE_OPTION = "New rule";
	// Holding a spinner arrow fires many changes; persist once it settles rather than on every step.
	private static final int SPINNER_COMMIT_DELAY_MS = 300;
	private static final String MORE_LABEL = "More ▾";
	private static final String COPIED_LABEL = "Copied!";
	private static final int COPIED_FLASH_MS = 1500;
	private static final String INTERFACE_STYLES_WARNING = "Interface Styles' 'High detail menu' or 'Menu alpha' setting "
		+ "conflicts with Better Right Click. Turn off High detail menu and set Menu alpha to 255, or turn off Interface Styles.";
	private static final int WARNING_PADDING = 6;
	// Before the first layout there is no width yet, so assume the sidebar's usual content width.
	private static final int WARNING_FALLBACK_WIDTH = PANEL_WIDTH - BORDER_OFFSET * 2;
	// Everything Reset all settings restores. Themes are kept, so their keys aren't here.
	private static final String[] GLOBAL_SETTING_KEYS = {
		BetterRightClickConfig.KEY_HIGHLIGHTS_ENABLED,
		BetterRightClickConfig.KEY_HIGHLIGHTS_BOLD,
		BetterRightClickConfig.KEY_HIGHLIGHT_RULES,
		BetterRightClickConfig.KEY_TARGET_MODE,
		BetterRightClickConfig.KEY_NPC_COLOUR,
		BetterRightClickConfig.KEY_PLAYER_COLOUR,
		BetterRightClickConfig.KEY_ITEM_COLOUR,
		BetterRightClickConfig.KEY_OBJECT_COLOUR,
		BetterRightClickConfig.KEY_OTHER_COLOUR,
		BetterRightClickConfig.KEY_COMBAT_LEVEL_MODE,
		BetterRightClickConfig.KEY_LOWER_LEVEL_COLOUR,
		BetterRightClickConfig.KEY_SAME_LEVEL_COLOUR,
		BetterRightClickConfig.KEY_HIGHER_LEVEL_COLOUR,
		BetterRightClickConfig.KEY_TEXT_OFFSET_X,
		BetterRightClickConfig.KEY_BASELINE_NUDGE,
		BetterRightClickConfig.KEY_SHOW_HIT_ROWS
	};
	private static final Consumer<Color> NOTHING = c ->
	{
	};

	private final ThemeManager themeManager;
	private final BetterRightClickConfig config;
	private final ConfigManager configManager;
	private final ColorPickerManager colorPickerManager;

	private final ThemePreview preview;
	private final JLabel interfaceStylesWarning = buildInterfaceStylesWarning();
	private final JComboBox<Theme> themeCombo = new JComboBox<>();
	private final JButton saveButton = new JButton("Save");
	private final JButton revertButton = new JButton("Revert");
	private final JButton moreButton = new JButton(MORE_LABEL);
	// Restarted on every export so a second click extends the flash instead of cutting it short.
	private final Timer copiedFlash = new Timer(COPIED_FLASH_MS, e -> moreButton.setText(MORE_LABEL));

	// Theme-bound controls; re-synced whenever the active/working theme changes.
	private final List<Runnable> themeControlRefreshers = new ArrayList<>();
	// Global controls; re-read from config after Reset all settings.
	private final List<Runnable> globalControlRefreshers = new ArrayList<>();

	private final JPanel rulesPanel = new JPanel();
	private final JButton addRuleButton = new JButton("+ Add rule");
	// EDT only; the renderer reads the persisted config string, never this list.
	private final List<EditableRule> rules = new ArrayList<>();

	private JSpinner fontSizeSpinner;
	private boolean refreshing;

	@Inject
	BetterRightClickPanel(LocalPlayerSnapshot localPlayer, ThemeManager themeManager, BetterRightClickConfig config,
		ConfigManager configManager, ColorPickerManager colorPickerManager)
	{
		this.themeManager = themeManager;
		this.config = config;
		this.configManager = configManager;
		this.colorPickerManager = colorPickerManager;

		preview = new ThemePreview(localPlayer, themeManager, config);

		CollapsibleSection previewSection = new CollapsibleSection("Preview", "Live preview of the active theme", false);
		previewSection.addRow(preview);
		add(previewSection);
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

	/**
	 * Shows or hides the Interface Styles conflict banner at the top of the panel. EDT only.
	 */
	void setInterfaceStylesWarningVisible(boolean visible)
	{
		// Removed rather than hidden: DynamicGridLayout still reserves space for invisible children.
		if (visible == (interfaceStylesWarning.getParent() == this))
		{
			return;
		}
		if (visible)
		{
			add(interfaceStylesWarning, 0);
		}
		else
		{
			remove(interfaceStylesWarning);
		}
		revalidate();
		repaint();
	}

	private static JLabel buildInterfaceStylesWarning()
	{
		JLabel label = new WrappingLabel("<html>" + INTERFACE_STYLES_WARNING + "</html>");
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(Color.BLACK);
		label.setBackground(ColorScheme.PROGRESS_INPROGRESS_COLOR);
		label.setOpaque(true);
		label.setBorder(BorderFactory.createEmptyBorder(WARNING_PADDING, WARNING_PADDING, WARNING_PADDING, WARNING_PADDING));
		return label;
	}

	/**
	 * An HTML label that wraps to the width the layout gives it and reports the matching height.
	 * A plain HTML label asks for one long line, so the layout clips the wrapped text.
	 */
	private static final class WrappingLabel extends JLabel
	{
		private View measuredView;
		private int measuredWidth;
		private int measuredHeight;
		private int laidOutWidth;

		private WrappingLabel(String html)
		{
			super(html);
			// The height depends on the width, which is only known after layout, so lay out once more
			// when the width changes (not on the height change that follows).
			addComponentListener(new ComponentAdapter()
			{
				@Override
				public void componentResized(ComponentEvent e)
				{
					if (getWidth() != laidOutWidth)
					{
						laidOutWidth = getWidth();
						revalidate();
					}
				}
			});
		}

		@Override
		public Dimension getPreferredSize()
		{
			View view = (View) getClientProperty(BasicHTML.propertyKey);
			if (view == null)
			{
				return super.getPreferredSize();
			}
			int width = getWidth() > 0 ? getWidth() : WARNING_FALLBACK_WIDTH;
			// The view is replaced when the text or font changes, so it doubles as a cache key.
			if (view != measuredView || width != measuredWidth)
			{
				Insets insets = getInsets();
				view.setSize(width - insets.left - insets.right, 0);
				measuredHeight = (int) Math.ceil(view.getPreferredSpan(View.Y_AXIS)) + insets.top + insets.bottom;
				measuredView = view;
				measuredWidth = width;
			}
			return new Dimension(width, measuredHeight);
		}
	}

	// --- Theme row: combo + save/revert/more ---

	private JPanel buildThemeRow()
	{
		JPanel container = new JPanel();
		container.setLayout(new DynamicGridLayout(0, 1, 0, ROW_GAP));

		themeCombo.setRenderer(new DefaultListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
			{
				Theme theme = (Theme) value;
				String text = theme == null ? "" : theme.builtIn ? "🔒 " + theme.name : theme.name;
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
			if (selected == null || selected.id.equals(themeManager.activeId()))
			{
				return;
			}
			if (!confirmDiscardEdits())
			{
				refreshing = true;
				try
				{
					selectActiveTheme();
				}
				finally
				{
					refreshing = false;
				}
				return;
			}
			themeManager.applyTheme(selected.id);
			refresh();
		});
		container.add(themeCombo);

		JPanel buttons = new JPanel(new GridLayout(2, 2, ROW_GAP, ROW_GAP));
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
		button.setMargin(COMPACT_INSETS);
		return button;
	}

	private JButton buildMoreButton()
	{
		JPopupMenu menu = new JPopupMenu();
		JMenuItem renameItem = new JMenuItem("Rename");
		renameItem.addActionListener(e -> promptRename());
		JMenuItem duplicateItem = new JMenuItem("Duplicate");
		duplicateItem.addActionListener(e ->
		{
			if (!confirmDiscardEdits())
			{
				return;
			}
			if (!themeManager.duplicate())
			{
				showThemesFull();
			}
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

		moreButton.setToolTipText("More theme actions");
		moreButton.addActionListener(e ->
		{
			boolean userTheme = !themeManager.active().builtIn;
			renameItem.setEnabled(userTheme);
			deleteItem.setEnabled(userTheme);
			menu.show(moreButton, 0, moreButton.getHeight());
		});
		copiedFlash.setRepeats(false);
		return moreButton;
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

		JSpinner radiusSpinner = themeIntSpinner(t -> t.radius, Theme::withRadius, Limits.MIN_RADIUS, Limits.MAX_RADIUS);
		section.addRow(row("Corner radius", radiusSpinner));

		JSpinner opacitySpinner = themeIntSpinner(t -> t.opacity, Theme::withOpacity, Limits.MIN_OPACITY, Limits.MAX_OPACITY);
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
			// RuneScape's bitmap fonts come in one size, so the size control would do nothing.
			fontSizeSpinner.setEnabled(selected != null && selected.isScalable());
			if (!refreshing)
			{
				apply(t -> t.withFont(selected));
			}
		});
		themeControlRefreshers.add(() -> fontCombo.setSelectedItem(themeManager.effectiveTheme().font));
		section.addRow(row("Font", fontCombo));

		fontSizeSpinner = themeIntSpinner(t -> t.fontSize, Theme::withFontSize, Limits.MIN_FONT_SIZE, Limits.MAX_FONT_SIZE);
		section.addRow(row("Font size", "Applies to the Sans and Serif fonts only", fontSizeSpinner));

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
		// Live edits only touch the working copy; themes are persisted by Save.
		ColorJButton button = colourButton(getter.apply(themeManager.effectiveTheme()), label,
			c -> apply(t -> setter.apply(t, c)), NOTHING);
		themeControlRefreshers.add(() -> setColourButton(button, getter.apply(themeManager.effectiveTheme())));
		return row(label, button);
	}

	private void apply(Function<Theme, Theme> mutator)
	{
		themeManager.setWorking(mutator.apply(themeManager.effectiveTheme()));
		preview.refresh();
		updateEditButtons();
	}

	private void updateEditButtons()
	{
		boolean edited = themeManager.isEdited();
		saveButton.setEnabled(edited && !themeManager.active().builtIn);
		revertButton.setEnabled(edited);
	}

	// --- Global sections ---

	private CollapsibleSection buildHighlightsSection()
	{
		CollapsibleSection section = new CollapsibleSection("Action highlights", "Highlight specific menu options", false);

		section.addRow(row("Enabled", globalCheckBox(config::highlightsEnabled, BetterRightClickConfig.KEY_HIGHLIGHTS_ENABLED)));
		section.addRow(row("Bold", globalCheckBox(config::highlightsBold, BetterRightClickConfig.KEY_HIGHLIGHTS_BOLD)));

		rulesPanel.setLayout(new DynamicGridLayout(0, 1, 0, ROW_GAP));
		loadRules();
		globalControlRefreshers.add(this::loadRules);
		section.addRow(rulesPanel);

		addRuleButton.addActionListener(e -> addNewRule());
		section.addRow(addRuleButton);

		return section;
	}

	private CollapsibleSection buildTargetsSection()
	{
		CollapsibleSection section = new CollapsibleSection("Targets", "Colour menu targets by entity type", false);

		ColorJButton npc = globalColourButton(config::npcColour, "NPC colour", BetterRightClickConfig.KEY_NPC_COLOUR);
		ColorJButton player = globalColourButton(config::playerColour, "Player colour", BetterRightClickConfig.KEY_PLAYER_COLOUR);
		ColorJButton item = globalColourButton(config::itemColour, "Item colour", BetterRightClickConfig.KEY_ITEM_COLOUR);
		ColorJButton object = globalColourButton(config::objectColour, "Object colour", BetterRightClickConfig.KEY_OBJECT_COLOUR);
		ColorJButton other = globalColourButton(config::otherColour, "Other colour", BetterRightClickConfig.KEY_OTHER_COLOUR);

		section.addRow(row("Mode", modeCombo(TargetMode.values(), config::targetMode, TargetMode.CUSTOM,
			BetterRightClickConfig.KEY_TARGET_MODE, npc, player, item, object, other)));
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

		ColorJButton lower = globalColourButton(config::lowerLevelColour, "Lower level colour", BetterRightClickConfig.KEY_LOWER_LEVEL_COLOUR);
		ColorJButton same = globalColourButton(config::sameLevelColour, "Same level colour", BetterRightClickConfig.KEY_SAME_LEVEL_COLOUR);
		ColorJButton higher = globalColourButton(config::higherLevelColour, "Higher level colour", BetterRightClickConfig.KEY_HIGHER_LEVEL_COLOUR);

		section.addRow(row("Mode", modeCombo(CombatLevelMode.values(), config::combatLevelMode, CombatLevelMode.CUSTOM,
			BetterRightClickConfig.KEY_COMBAT_LEVEL_MODE, lower, same, higher)));
		section.addRow(row("Lower level", lower));
		section.addRow(row("Same level", same));
		section.addRow(row("Higher level", higher));
		return section;
	}

	private CollapsibleSection buildAdvancedSection()
	{
		CollapsibleSection section = new CollapsibleSection("Advanced", "Geometry and debugging", true);

		section.addRow(row("Text offset X", globalIntSpinner(() -> Limits.textOffsetX(config),
			Limits.MIN_TEXT_OFFSET_X, Limits.MAX_TEXT_OFFSET_X, BetterRightClickConfig.KEY_TEXT_OFFSET_X)));
		section.addRow(row("Baseline nudge", globalIntSpinner(() -> Limits.baselineNudge(config),
			Limits.MIN_BASELINE_NUDGE, Limits.MAX_BASELINE_NUDGE, BetterRightClickConfig.KEY_BASELINE_NUDGE)));

		section.addRow(row("Show hit rows", globalCheckBox(config::showHitRows, BetterRightClickConfig.KEY_SHOW_HIT_ROWS)));

		JButton reset = new JButton("Reset all settings");
		reset.addActionListener(e -> promptResetAll());
		section.addRow(reset);

		return section;
	}

	/**
	 * Saves a global (non-theme) setting. The preview reads these from config, so refresh it after.
	 */
	private void setGlobal(String key, Object value)
	{
		configManager.setConfiguration(BetterRightClickConfig.GROUP, key, value);
		preview.refresh();
	}

	private ColorJButton globalColourButton(Supplier<Color> getter, String title, String key)
	{
		ColorJButton button = colourButton(getter.get(), title, NOTHING, c -> setGlobal(key, c));
		globalControlRefreshers.add(() -> setColourButton(button, getter.get()));
		return button;
	}

	private JCheckBox globalCheckBox(BooleanSupplier getter, String key)
	{
		JCheckBox checkbox = new JCheckBox();
		checkbox.setSelected(getter.getAsBoolean());
		// setSelected doesn't fire action events, so refreshing can't write back to config.
		checkbox.addActionListener(e -> setGlobal(key, checkbox.isSelected()));
		globalControlRefreshers.add(() -> checkbox.setSelected(getter.getAsBoolean()));
		return checkbox;
	}

	/**
	 * A global mode combo whose colour buttons are only enabled in the mode that uses them.
	 */
	private <E extends Enum<E>> JComboBox<E> modeCombo(E[] modes, Supplier<E> getter, E customMode, String key,
		ColorJButton... colourButtons)
	{
		JComboBox<E> combo = new JComboBox<>(modes);
		combo.setRenderer(new TitleCaseListCellRenderer());
		combo.setSelectedItem(getter.get());
		Runnable updateEnabled = () ->
		{
			boolean custom = combo.getSelectedItem() == customMode;
			for (ColorJButton button : colourButtons)
			{
				button.setEnabled(custom);
			}
		};
		updateEnabled.run();
		combo.addActionListener(e ->
		{
			if (!refreshing)
			{
				setGlobal(key, combo.getSelectedItem());
			}
			updateEnabled.run();
		});
		globalControlRefreshers.add(() -> combo.setSelectedItem(getter.get()));
		return combo;
	}

	private JSpinner globalIntSpinner(IntSupplier getter, int min, int max, String key)
	{
		JSpinner spinner = new JSpinner(new SpinnerNumberModel(getter.getAsInt(), min, max, 1));
		spinnerFieldWidth(spinner);
		Timer commit = new Timer(SPINNER_COMMIT_DELAY_MS, e -> setGlobal(key, spinner.getValue()));
		commit.setRepeats(false);
		spinner.addChangeListener(e ->
		{
			if (!refreshing)
			{
				commit.restart();
			}
		});
		globalControlRefreshers.add(() ->
		{
			// A pending edit would otherwise land after the reset and undo it.
			commit.stop();
			spinner.setValue(getter.getAsInt());
		});
		return spinner;
	}

	// --- Action highlight rules editor ---

	private static final class EditableRule
	{
		private String option;
		private Color colour;

		private EditableRule(String option, Color colour)
		{
			this.option = option;
			this.colour = colour;
		}
	}

	/**
	 * Writes every current rule to config, skipping blank option names. Blank rows stay in the
	 * UI while they're being edited; they just aren't saved yet.
	 */
	private void persistRules()
	{
		StringBuilder sb = new StringBuilder();
		for (EditableRule rule : rules)
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
		setGlobal(BetterRightClickConfig.KEY_HIGHLIGHT_RULES, sb.toString());
	}

	private void loadRules()
	{
		rules.clear();
		rulesPanel.removeAll();
		for (HighlightRules.Rule parsed : HighlightRules.parse(config.highlightRules()))
		{
			EditableRule rule = new EditableRule(parsed.option, parsed.colour);
			rules.add(rule);
			rulesPanel.add(ruleRow(rule, false));
		}
		rulesPanel.revalidate();
		rulesPanel.repaint();
		updateAddRuleEnabled();
	}

	private void addNewRule()
	{
		if (rules.size() >= Limits.MAX_RULES)
		{
			return;
		}
		EditableRule rule = new EditableRule(NEW_RULE_OPTION, NEW_RULE_COLOUR);
		rules.add(rule);
		rulesPanel.add(ruleRow(rule, true));
		rulesPanel.revalidate();
		rulesPanel.repaint();
		updateAddRuleEnabled();
	}

	private void updateAddRuleEnabled()
	{
		addRuleButton.setEnabled(rules.size() < Limits.MAX_RULES);
	}

	private JPanel ruleRow(EditableRule rule, boolean focusField)
	{
		JPanel panel = new JPanel(new BorderLayout(ROW_GAP, 0));

		JTextField optionField = new JTextField(rule.option);
		optionField.setBorder(BorderFactory.createEmptyBorder(FIELD_PADDING, FIELD_PADDING, FIELD_PADDING, FIELD_PADDING));
		Runnable commit = () ->
		{
			String option = Limits.truncate(optionField.getText().trim(), Limits.MAX_RULE_OPTION_LENGTH);
			// Enter followed by focus loss would otherwise write the same value twice
			if (!option.equals(rule.option))
			{
				rule.option = option;
				persistRules();
			}
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

		ColorJButton colourBtn = colourButton(rule.colour, "Rule colour", c -> rule.colour = c, c -> persistRules());

		JButton remove = new JButton("✕");
		remove.setToolTipText("Remove rule");
		remove.setMargin(REMOVE_INSETS);
		remove.addActionListener(e ->
		{
			rules.remove(rule);
			rulesPanel.remove(panel);
			rulesPanel.revalidate();
			rulesPanel.repaint();
			persistRules();
			updateAddRuleEnabled();
		});

		JPanel right = new JPanel(new BorderLayout(ROW_GAP, 0));
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

	/**
	 * @param onChange called live while the picker is dragged; must not persist
	 * @param onCommit called once when the picker closes
	 */
	private ColorJButton colourButton(Color initial, String title, Consumer<Color> onChange, Consumer<Color> onCommit)
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
				picker.setOnClose(onCommit);
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
		return "#" + ColorUtil.colorToHexCode(colour).toUpperCase(Locale.ROOT);
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
			((JSpinner.DefaultEditor) editor).getTextField().setColumns(SPINNER_COLUMNS);
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

			selectActiveTheme();

			for (Runnable r : themeControlRefreshers)
			{
				r.run();
			}

			updateEditButtons();
			preview.refresh();
		}
		finally
		{
			refreshing = false;
		}
	}

	// Callers must hold the refreshing guard so the combo's listener ignores this change.
	private void selectActiveTheme()
	{
		String activeId = themeManager.activeId();
		for (int i = 0; i < themeCombo.getItemCount(); i++)
		{
			if (themeCombo.getItemAt(i).id.equals(activeId))
			{
				themeCombo.setSelectedIndex(i);
				return;
			}
		}
	}

	/**
	 * @return true if there are no unsaved edits, or the user chose to discard them
	 */
	private boolean confirmDiscardEdits()
	{
		if (!themeManager.isEdited())
		{
			return true;
		}
		return confirm("Discard unsaved changes to " + themeManager.active().name + "?", "Unsaved changes", "Discard");
	}

	/**
	 * A warning dialog with {@code action} and Cancel, where Cancel has the focus.
	 *
	 * @return true if the user chose {@code action}
	 */
	private boolean confirm(String message, String title, String action)
	{
		Object[] options = {action, "Cancel"};
		return JOptionPane.showOptionDialog(this, message, title, JOptionPane.DEFAULT_OPTION,
			JOptionPane.WARNING_MESSAGE, null, options, options[1]) == 0;
	}

	/**
	 * Restores every global setting to its default and switches to the default theme, dropping
	 * unsaved edits. Saved themes are kept.
	 */
	private void promptResetAll()
	{
		if (!confirm("Reset all settings to default? Your saved themes are kept.", "Reset all settings", "Reset"))
		{
			return;
		}
		for (String key : GLOBAL_SETTING_KEYS)
		{
			configManager.unsetConfiguration(BetterRightClickConfig.GROUP, key);
		}
		themeManager.applyTheme(BuiltInThemes.DEFAULT_ID);

		refreshing = true;
		try
		{
			for (Runnable r : globalControlRefreshers)
			{
				r.run();
			}
		}
		finally
		{
			refreshing = false;
		}
		refresh();
	}

	private void promptSaveAsNew()
	{
		String name = JOptionPane.showInputDialog(this, "Theme name:", "Save as new theme", JOptionPane.PLAIN_MESSAGE);
		if (name != null && !name.trim().isEmpty())
		{
			if (!themeManager.saveAsNew(name))
			{
				showThemesFull();
			}
			refresh();
		}
	}

	private void promptRename()
	{
		String name = JOptionPane.showInputDialog(this, "New name:", themeManager.active().name);
		if (name != null && !name.trim().isEmpty())
		{
			themeManager.rename(name);
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
		try
		{
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(payload), null);
		}
		catch (RuntimeException e)
		{
			// the clipboard is OS-owned and can be unavailable or busy
			JOptionPane.showMessageDialog(this, "Couldn't copy the theme to the clipboard.", "Export failed",
				JOptionPane.ERROR_MESSAGE);
			return;
		}
		moreButton.setText(COPIED_LABEL);
		copiedFlash.restart();
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
		catch (UnsupportedFlavorException | IOException | RuntimeException ignored)
		{
			// the clipboard is OS-owned and can fail in many ways; just leave the field blank
		}

		String payload = (String) JOptionPane.showInputDialog(this, "Paste exported theme:", "Import theme",
			JOptionPane.PLAIN_MESSAGE, null, null, clipboardText);
		if (payload == null || payload.trim().isEmpty() || !confirmDiscardEdits())
		{
			return;
		}

		String error = themeManager.importTheme(payload);
		if (error != null)
		{
			JOptionPane.showMessageDialog(this, error, "Import failed", JOptionPane.ERROR_MESSAGE);
			return;
		}
		refresh();
	}

	private void showThemesFull()
	{
		JOptionPane.showMessageDialog(this, ThemeManager.ERROR_FULL, "Too many themes", JOptionPane.WARNING_MESSAGE);
	}
}
