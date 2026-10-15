package com.betterrightclick;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

@Slf4j
class MenuOverlay extends Overlay
{
	private static final int NO_ITEM = -1;

	private final Client client;
	private final BetterRightClickConfig config;
	private final ThemeManager themeManager;
	private final MenuRenderer renderer = new MenuRenderer();
	private final List<MenuRenderer.Row> rows = new ArrayList<>();
	private final List<MenuRenderer.Row> rowPool = new ArrayList<>();
	private final Rectangle bounds = new Rectangle();

	/**
	 * Set after the first render failure. The plugin consumes {@code BeforeMenuRender}, so a render
	 * that throws would leave the menu invisible; once this is set we stop consuming and the vanilla
	 * menu is drawn for the rest of the session. Volatile because the plugin resets it from startUp.
	 */
	private volatile boolean failed;

	@Inject
	private MenuOverlay(Client client, BetterRightClickConfig config, ThemeManager themeManager)
	{
		this.client = client;
		this.config = config;
		this.themeManager = themeManager;
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
		setPosition(OverlayPosition.DYNAMIC);
	}

	void resetFailure()
	{
		failed = false;
	}

	/**
	 * Whether we should replace the vanilla menu right now. Submenus are left to vanilla: the API
	 * exposes no way to tell whether a submenu is open, so any menu containing one is drawn natively.
	 */
	boolean shouldDraw()
	{
		if (failed || !client.isMenuOpen())
		{
			return false;
		}
		Menu menu = client.getMenu();
		if (menu == null)
		{
			return false;
		}
		MenuEntry[] entries = menu.getMenuEntries();
		if (entries == null || entries.length == 0)
		{
			return false;
		}
		for (MenuEntry entry : entries)
		{
			if (entry != null && entry.getSubMenu() != null)
			{
				return false;
			}
		}
		return true;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!shouldDraw())
		{
			return null;
		}
		try
		{
			renderMenu(graphics, client.getMenu());
		}
		catch (RuntimeException e)
		{
			failed = true;
			log.warn("Better Right Click failed to draw the menu; using the vanilla menu for the rest of this session", e);
		}
		return null;
	}

	private void renderMenu(Graphics2D graphics, Menu menu)
	{
		bounds.setBounds(menu.getMenuX(), menu.getMenuY(), menu.getMenuWidth(), menu.getMenuHeight());
		Theme theme = themeManager.effectiveTheme();

		fillRows(menu.getMenuEntries());
		if (config.combatLevelMode() == CombatLevelMode.HIDE)
		{
			// The game's width counts the hidden levels; X, Y and row heights still come from the game.
			bounds.width = Math.min(bounds.width, renderer.measureWidth(graphics, client, theme, config, rows));
		}

		int headerHeight = theme.showHeader ? Limits.headerHeight(config) : 0;
		int hoverIndex = hoverIndex(client.getMouseCanvasPosition(), headerHeight, Limits.rowHeight(config));
		Player local = client.getLocalPlayer();
		int localLevel = local != null ? local.getCombatLevel() : 0;
		renderer.render(graphics, client, theme, config, rows, bounds, hoverIndex, localLevel);
	}

	/**
	 * Entries are stored bottom-up; rows are drawn top-down. A null entry still occupies a row so
	 * the hover maths stays aligned with the vanilla click areas.
	 */
	private void fillRows(MenuEntry[] entries)
	{
		rows.clear();
		for (int i = entries.length - 1; i >= 0; i--)
		{
			int slot = rows.size();
			if (slot == rowPool.size())
			{
				rowPool.add(new MenuRenderer.Row(null, null, MenuRenderer.TargetKind.OTHER));
			}
			MenuRenderer.Row row = rowPool.get(slot);
			MenuEntry entry = entries[i];
			if (entry != null)
			{
				row.set(entry.getOption(), entry.getTarget(), classify(entry));
			}
			else
			{
				row.set(null, null, MenuRenderer.TargetKind.OTHER);
			}
			rows.add(row);
		}
	}

	private int hoverIndex(Point mouse, int headerHeight, int rowHeight)
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
		int index = relativeY / rowHeight;
		return index < rows.size() ? index : -1;
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
		if (entry.getItemId() != NO_ITEM)
		{
			return MenuRenderer.TargetKind.ITEM;
		}
		MenuAction type = entry.getType();
		if (type == null)
		{
			return MenuRenderer.TargetKind.OTHER;
		}
		switch (type)
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
