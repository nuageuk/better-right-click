package com.betterrightclick;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;

/**
 * Draws a fixed sample menu with the current working theme, for live feedback while editing. Paints
 * on the EDT, so it never touches {@code Client}; player details come from {@link LocalPlayerSnapshot}.
 */
class ThemePreview extends JPanel
{
	private static final int TOP_MARGIN = 10;
	private static final int SIDE_MARGIN = 8;
	private static final int MIN_MENU_WIDTH = 10;
	// Lowest possible combat level; used while logged out.
	private static final int PLACEHOLDER_COMBAT_LEVEL = 3;

	private static final List<MenuRenderer.Row> SAMPLE_ROWS = Arrays.asList(
		new MenuRenderer.Row("Attack", "<col=ffff00>Guard</col> <col=ffffff>(level-21)</col>", MenuRenderer.TargetKind.NPC),
		new MenuRenderer.Row("Pickpocket", "<col=ffff00>Guard</col>", MenuRenderer.TargetKind.NPC),
		new MenuRenderer.Row("Use", "<col=ff9040>Lobster</col>", MenuRenderer.TargetKind.ITEM),
		new MenuRenderer.Row("Walk here", "", MenuRenderer.TargetKind.OTHER),
		new MenuRenderer.Row("Examine", "<col=ffff00>Guard</col>", MenuRenderer.TargetKind.NPC),
		new MenuRenderer.Row("Cancel", "", MenuRenderer.TargetKind.OTHER)
	);

	private final LocalPlayerSnapshot localPlayer;
	private final ThemeManager themeManager;
	private final BetterRightClickConfig config;
	private final MenuRenderer renderer = new MenuRenderer();
	private final Rectangle bounds = new Rectangle();

	private int hoverIndex = -1;

	ThemePreview(LocalPlayerSnapshot localPlayer, ThemeManager themeManager, BetterRightClickConfig config)
	{
		this.localPlayer = localPlayer;
		this.themeManager = themeManager;
		this.config = config;
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		localPlayer.setOnChange(this::repaint);

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

	/**
	 * Call after any change that can affect the drawn menu; header/row heights change its size.
	 */
	void refresh()
	{
		revalidate();
		repaint();
	}

	@Override
	public Dimension getPreferredSize()
	{
		return new Dimension(getWidth(), rowsTop(themeManager.effectiveTheme()) + rowsHeight() + TOP_MARGIN);
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		Theme theme = themeManager.effectiveTheme();
		int width = Math.max(MIN_MENU_WIDTH, getWidth() - SIDE_MARGIN * 2);
		// The renderer always leaves room for the header, so shift up when it's hidden.
		bounds.setBounds(SIDE_MARGIN, rowsTop(theme) - MenuRenderer.HEADER_HEIGHT, width,
			MenuRenderer.HEADER_HEIGHT + rowsHeight());
		Integer level = localPlayer.combatLevel();
		renderer.render((Graphics2D) g, null, theme, config, SAMPLE_ROWS, bounds, hoverIndex,
			level != null ? level : PLACEHOLDER_COMBAT_LEVEL);
	}

	private void updateHover(int mouseY)
	{
		int relativeY = mouseY - rowsTop(themeManager.effectiveTheme());
		int index = relativeY < 0 ? -1 : relativeY / MenuRenderer.ROW_HEIGHT;
		setHover(index < SAMPLE_ROWS.size() ? index : -1);
	}

	private void setHover(int index)
	{
		if (index != hoverIndex)
		{
			hoverIndex = index;
			repaint();
		}
	}

	private static int rowsTop(Theme theme)
	{
		return TOP_MARGIN + (theme.showHeader ? MenuRenderer.HEADER_HEIGHT : 0);
	}

	private static int rowsHeight()
	{
		return MenuRenderer.ROW_HEIGHT * SAMPLE_ROWS.size();
	}
}
