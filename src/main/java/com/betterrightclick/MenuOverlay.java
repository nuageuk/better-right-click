package com.betterrightclick;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Menu;
import net.runelite.api.MenuEntry;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class MenuOverlay extends Overlay
{
	private final Client client;
	private final BetterRightClickConfig config;
	private final ThemeManager themeManager;

	@Inject
	private MenuOverlay(Client client, BetterRightClickConfig config, ThemeManager themeManager)
	{
		this.client = client;
		this.config = config;
		this.themeManager = themeManager;
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
		setPosition(OverlayPosition.DYNAMIC);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!client.isMenuOpen())
		{
			return null;
		}

		Menu menu = client.getMenu();
		Rectangle bounds = new Rectangle(menu.getMenuX(), menu.getMenuY(), menu.getMenuWidth(), menu.getMenuHeight());
		Theme theme = themeManager.effectiveTheme();

		MenuEntry[] entries = menu.getMenuEntries();
		List<MenuRenderer.Row> rows = new ArrayList<>(entries.length);
		for (int i = entries.length - 1; i >= 0; i--)
		{
			MenuEntry entry = entries[i];
			rows.add(new MenuRenderer.Row(entry.getOption(), entry.getTarget(), classify(entry)));
		}

		int headerHeight = theme.showHeader ? config.headerHeight() : 0;
		int hoverIndex = hoverIndex(client.getMouseCanvasPosition(), bounds, headerHeight, config.rowHeight(), rows.size());

		MenuRenderer.render(graphics, client, theme, config, rows, bounds, hoverIndex);
		return null;
	}

	private static int hoverIndex(Point mouse, Rectangle bounds, int headerHeight, int rowHeight, int rowCount)
	{
		if (mouse == null || mouse.getX() < bounds.x || mouse.getX() >= bounds.x + bounds.width)
		{
			return -1;
		}
		int relativeY = mouse.getY() - (bounds.y + headerHeight);
		if (relativeY < 0)
		{
			return -1;
		}
		int idx = relativeY / rowHeight;
		return idx < rowCount ? idx : -1;
	}

	private static MenuRenderer.TargetKind classify(MenuEntry entry)
	{
		if (entry.getNpc() != null)
		{
			return MenuRenderer.TargetKind.NPC;
		}
		if (entry.getPlayer() != null)
		{
			return MenuRenderer.TargetKind.PLAYER;
		}
		if (entry.getItemId() != -1)
		{
			return MenuRenderer.TargetKind.ITEM;
		}
		switch (entry.getType())
		{
			case GAME_OBJECT_FIRST_OPTION:
			case GAME_OBJECT_SECOND_OPTION:
			case GAME_OBJECT_THIRD_OPTION:
			case GAME_OBJECT_FOURTH_OPTION:
			case GAME_OBJECT_FIFTH_OPTION:
				return MenuRenderer.TargetKind.OBJECT;
			default:
				return MenuRenderer.TargetKind.OTHER;
		}
	}
}
