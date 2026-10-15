package com.menuthemes;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;
import javax.swing.JPanel;
import net.runelite.api.Client;
import net.runelite.client.ui.ColorScheme;

/**
 * Draws a fixed sample menu with the current working theme, for live feedback while editing.
 */
class ThemePreview extends JPanel
{
	private static final int TOP_MARGIN = 10;
	private static final int SIDE_MARGIN = 10;

	private static final List<MenuRenderer.Row> SAMPLE_ROWS = Arrays.asList(
		new MenuRenderer.Row("Attack", "<col=ffff00>Guard</col> <col=ffffff>(level-21)</col>", MenuRenderer.TargetKind.NPC),
		new MenuRenderer.Row("Pickpocket", "<col=ffff00>Guard</col>", MenuRenderer.TargetKind.NPC),
		new MenuRenderer.Row("Use", "<col=ff9040>Lobster</col>", MenuRenderer.TargetKind.ITEM),
		new MenuRenderer.Row("Walk here", "", MenuRenderer.TargetKind.OTHER),
		new MenuRenderer.Row("Examine", "<col=ffff00>Guard</col>", MenuRenderer.TargetKind.NPC),
		new MenuRenderer.Row("Cancel", "", MenuRenderer.TargetKind.OTHER)
	);

	private final Client client;
	private final ThemeManager themeManager;
	private final MenuThemesConfig config;
	private int hoverIndex = -1;

	ThemePreview(Client client, ThemeManager themeManager, MenuThemesConfig config)
	{
		this.client = client;
		this.themeManager = themeManager;
		this.config = config;
		setBackground(ColorScheme.DARKER_GRAY_COLOR);

		MouseAdapter mouse = new MouseAdapter()
		{
			@Override
			public void mouseMoved(MouseEvent e)
			{
				updateHover(e.getY());
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				setHover(-1);
			}
		};
		addMouseMotionListener(mouse);
		addMouseListener(mouse);
	}

	private void updateHover(int mouseY)
	{
		Theme theme = themeManager.effectiveTheme();
		int headerHeight = theme.showHeader ? config.headerHeight() : 0;
		int relativeY = mouseY - (TOP_MARGIN + headerHeight);
		int idx = relativeY < 0 ? -1 : relativeY / config.rowHeight();
		setHover(idx >= 0 && idx < SAMPLE_ROWS.size() ? idx : -1);
	}

	private void setHover(int index)
	{
		if (index != hoverIndex)
		{
			hoverIndex = index;
			repaint();
		}
	}

	private int menuHeight(Theme theme)
	{
		int headerHeight = theme.showHeader ? config.headerHeight() : 0;
		return headerHeight + config.rowHeight() * SAMPLE_ROWS.size();
	}

	@Override
	public Dimension getPreferredSize()
	{
		return new Dimension(getWidth(), menuHeight(themeManager.effectiveTheme()) + TOP_MARGIN * 2);
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		Theme theme = themeManager.effectiveTheme();
		int width = Math.max(10, getWidth() - SIDE_MARGIN * 2);
		Rectangle bounds = new Rectangle(SIDE_MARGIN, TOP_MARGIN, width, menuHeight(theme));
		MenuRenderer.render((Graphics2D) g, client, theme, config, SAMPLE_ROWS, bounds, hoverIndex);
	}
}
