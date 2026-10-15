package com.menuthemes;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import net.runelite.api.Client;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.ui.components.colorpicker.RuneliteColorPicker;

class MenuThemesPanel extends PluginPanel
{
	private final Client client;
	private final ThemeManager themeManager;
	private final MenuThemesConfig config;
	private final ColorPickerManager colorPickerManager;

	private final ThemePreview preview;
	private final DefaultListModel<Theme> listModel = new DefaultListModel<>();
	private final JList<Theme> themeList = new JList<>(listModel);

	private final List<Runnable> swatchRefreshers = new ArrayList<>();
	private final JSpinner radiusSpinner = new JSpinner(new SpinnerNumberModel(6, 0, 8, 1));
	private final JSpinner opacitySpinner = new JSpinner(new SpinnerNumberModel(96, 40, 100, 1));
	private final JComboBox<HoverStyle> hoverStyleCombo = new JComboBox<>(HoverStyle.values());
	private final JCheckBox borderCheck = new JCheckBox("Border");
	private final JCheckBox shadowCheck = new JCheckBox("Drop shadow");
	private final JCheckBox headerCheck = new JCheckBox("Header");

	private final JButton saveButton = new JButton("Save");
	private final JButton renameButton = new JButton("Rename");
	private final JButton deleteButton = new JButton("Delete");

	private boolean refreshing;

	@Inject
	MenuThemesPanel(Client client, ThemeManager themeManager, MenuThemesConfig config,
		ColorPickerManager colorPickerManager)
	{
		this.client = client;
		this.themeManager = themeManager;
		this.config = config;
		this.colorPickerManager = colorPickerManager;

		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(8, 0, 0, 0));

		preview = new ThemePreview(client, themeManager, config);

		JPanel content = new JPanel();
		content.setLayout(new DynamicGridLayout(0, 1, 0, 8));
		content.add(preview);
		content.add(new JSeparator());
		content.add(buildThemeListPanel());
		content.add(new JSeparator());
		content.add(buildEditorPanel());
		content.add(buildActionButtons());

		add(content, BorderLayout.NORTH);

		refresh();
	}

	private JPanel buildThemeListPanel()
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel title = titleLabel("Themes");
		panel.add(title, BorderLayout.NORTH);

		themeList.setCellRenderer((list, theme, index, isSelected, hasFocus) ->
		{
			boolean active = theme.id.equals(themeManager.activeId());
			StringBuilder text = new StringBuilder();
			if (theme.builtIn)
			{
				text.append("🔒 ");
			}
			text.append(theme.name);
			if (active && themeManager.isEdited())
			{
				text.append("  • edited");
			}

			JLabel label = new JLabel(text.toString());
			label.setOpaque(true);
			label.setBorder(new EmptyBorder(4, 8, 4, 8));
			label.setFont(FontManager.getRunescapeSmallFont());
			label.setBackground(isSelected ? ColorScheme.DARK_GRAY_HOVER_COLOR : ColorScheme.DARKER_GRAY_COLOR);
			label.setForeground(active ? Color.WHITE : ColorScheme.LIGHT_GRAY_COLOR);
			return label;
		});
		themeList.setFixedCellHeight(24);
		themeList.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		themeList.addListSelectionListener(e ->
		{
			if (refreshing || e.getValueIsAdjusting())
			{
				return;
			}
			Theme selected = themeList.getSelectedValue();
			if (selected != null && !selected.id.equals(themeManager.activeId()))
			{
				themeManager.applyTheme(selected.id);
				refresh();
			}
		});

		panel.add(themeList, BorderLayout.CENTER);
		return panel;
	}

	private JPanel buildEditorPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new DynamicGridLayout(0, 1, 0, 4));
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		panel.add(titleLabel("Colours"));

		addColourRow(panel, "Background", t -> t.bg, Theme::withBg);
		addColourRow(panel, "Border", t -> t.border, Theme::withBorder);
		addColourRow(panel, "Header", t -> t.header, Theme::withHeader);
		addColourRow(panel, "Header text", t -> t.headerText, Theme::withHeaderText);
		addColourRow(panel, "Option text", t -> t.text, Theme::withText);
		addColourRow(panel, "Hover", t -> t.hover, Theme::withHover);
		addColourRow(panel, "Accent", t -> t.accent, Theme::withAccent);

		panel.add(titleLabel("Style"));

		JPanel radiusRow = labelled("Corner radius", radiusSpinner);
		radiusSpinner.addChangeListener(e -> apply(t -> t.withRadius((Integer) radiusSpinner.getValue())));
		panel.add(radiusRow);

		JPanel opacityRow = labelled("Opacity", opacitySpinner);
		opacitySpinner.addChangeListener(e -> apply(t -> t.withOpacity((Integer) opacitySpinner.getValue())));
		panel.add(opacityRow);

		JPanel hoverRow = labelled("Hover style", hoverStyleCombo);
		hoverStyleCombo.addActionListener(e -> apply(t -> t.withHoverStyle((HoverStyle) hoverStyleCombo.getSelectedItem())));
		panel.add(hoverRow);

		borderCheck.setOpaque(false);
		borderCheck.setForeground(Color.WHITE);
		borderCheck.addActionListener(e -> apply(t -> t.withShowBorder(borderCheck.isSelected())));
		panel.add(borderCheck);

		shadowCheck.setOpaque(false);
		shadowCheck.setForeground(Color.WHITE);
		shadowCheck.addActionListener(e -> apply(t -> t.withShowShadow(shadowCheck.isSelected())));
		panel.add(shadowCheck);

		headerCheck.setOpaque(false);
		headerCheck.setForeground(Color.WHITE);
		headerCheck.addActionListener(e -> apply(t -> t.withShowHeader(headerCheck.isSelected())));
		panel.add(headerCheck);

		return panel;
	}

	private JPanel buildActionButtons()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new DynamicGridLayout(0, 2, 4, 4));
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		saveButton.addActionListener(e ->
		{
			themeManager.save();
			refresh();
		});
		panel.add(saveButton);

		JButton saveAsButton = new JButton("Save as new…");
		saveAsButton.addActionListener(e -> promptSaveAsNew());
		panel.add(saveAsButton);

		JButton revertButton = new JButton("Revert");
		revertButton.addActionListener(e ->
		{
			themeManager.revert();
			refresh();
		});
		panel.add(revertButton);

		renameButton.addActionListener(e -> promptRename());
		panel.add(renameButton);

		JButton duplicateButton = new JButton("Duplicate");
		duplicateButton.addActionListener(e ->
		{
			themeManager.duplicate();
			refresh();
		});
		panel.add(duplicateButton);

		deleteButton.addActionListener(e -> promptDelete());
		panel.add(deleteButton);

		JButton exportButton = new JButton("Export");
		exportButton.addActionListener(e -> exportActive(exportButton));
		panel.add(exportButton);

		JButton importButton = new JButton("Import");
		importButton.addActionListener(e -> promptImport());
		panel.add(importButton);

		return panel;
	}

	private void addColourRow(JPanel container, String label, Function<Theme, Color> getter,
		BiFunction<Theme, Color, Theme> setter)
	{
		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel text = new JLabel(label);
		text.setForeground(Color.WHITE);
		text.setFont(FontManager.getRunescapeSmallFont());

		JPanel swatch = new JPanel();
		swatch.setPreferredSize(new Dimension(24, 20));
		swatch.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR));
		swatch.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		swatch.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				Color current = getter.apply(themeManager.effectiveTheme());
				RuneliteColorPicker picker = colorPickerManager.create(swatch, current, label, true);
				picker.setOnColorChange(c ->
				{
					apply(t -> setter.apply(t, c));
					swatch.setBackground(c);
				});
				picker.setVisible(true);
			}
		});

		swatchRefreshers.add(() -> swatch.setBackground(getter.apply(themeManager.effectiveTheme())));

		row.add(text, BorderLayout.WEST);
		row.add(swatch, BorderLayout.EAST);
		container.add(row);
	}

	private static JPanel labelled(String label, Component control)
	{
		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(ColorScheme.DARK_GRAY_COLOR);
		JLabel text = new JLabel(label);
		text.setForeground(Color.WHITE);
		text.setFont(FontManager.getRunescapeSmallFont());
		row.add(text, BorderLayout.WEST);
		row.add(control, BorderLayout.EAST);
		return row;
	}

	private static JLabel titleLabel(String text)
	{
		JLabel label = new JLabel(text);
		label.setForeground(Color.WHITE);
		label.setFont(FontManager.getRunescapeBoldFont());
		return label;
	}

	/**
	 * Applies a single edit to the working copy and refreshes the dependent views.
	 */
	private void apply(Function<Theme, Theme> mutator)
	{
		themeManager.setWorking(mutator.apply(themeManager.effectiveTheme()));
		preview.repaint();
		themeList.repaint();
		updateButtonStates();
	}

	private void refresh()
	{
		refreshing = true;
		try
		{
			listModel.clear();
			for (Theme theme : themeManager.builtIns())
			{
				listModel.addElement(theme);
			}
			for (Theme theme : themeManager.userThemes())
			{
				listModel.addElement(theme);
			}

			Theme active = themeManager.active();
			for (int i = 0; i < listModel.size(); i++)
			{
				if (listModel.get(i).id.equals(active.id))
				{
					themeList.setSelectedIndex(i);
					break;
				}
			}

			Theme working = themeManager.effectiveTheme();
			for (Runnable r : swatchRefreshers)
			{
				r.run();
			}
			radiusSpinner.setValue(working.radius);
			opacitySpinner.setValue(working.opacity);
			hoverStyleCombo.setSelectedItem(working.hoverStyle);
			borderCheck.setSelected(working.showBorder);
			shadowCheck.setSelected(working.showShadow);
			headerCheck.setSelected(working.showHeader);

			updateButtonStates();
			preview.repaint();
			themeList.repaint();
		}
		finally
		{
			refreshing = false;
		}
	}

	private void updateButtonStates()
	{
		boolean userTheme = !themeManager.active().builtIn;
		saveButton.setEnabled(userTheme && themeManager.isEdited());
		renameButton.setEnabled(userTheme);
		deleteButton.setEnabled(userTheme);
	}

	private void promptSaveAsNew()
	{
		String name = JOptionPane.showInputDialog(this, "Theme name:", "Save as new theme",
			JOptionPane.PLAIN_MESSAGE);
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

	private void exportActive(JButton button)
	{
		String payload = themeManager.export(themeManager.activeId());
		if (payload == null)
		{
			return;
		}
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(new StringSelection(payload), null);

		String original = button.getText();
		button.setText("Copied!");
		button.setEnabled(false);
		Timer resetTimer = new Timer(1200, e ->
		{
			button.setText(original);
			button.setEnabled(true);
		});
		resetTimer.setRepeats(false);
		resetTimer.start();
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
