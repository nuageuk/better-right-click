package com.rightclickstyler;

import java.awt.Rectangle;
import java.util.function.Consumer;
import net.runelite.api.Actor;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.widgets.Widget;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MenuOverlayTest
{
	@Test
	public void usesCompleteNativeMenuBounds()
	{
		Rectangle bounds = new Rectangle();

		MenuOverlay.setNativeBounds(bounds, new StubMenu(12, 34, 207, 79));

		assertEquals(new Rectangle(12, 34, 207, 79), bounds);
	}

	@Test
	public void fallsBackBeforeInspectingScrollableMenus()
	{
		assertTrue(MenuOverlay.canInspectMenu(false, true, false));
		assertFalse(MenuOverlay.canInspectMenu(false, true, true));
		assertFalse(MenuOverlay.canInspectMenu(true, true, false));
		assertFalse(MenuOverlay.canInspectMenu(false, false, false));
	}

	@Test
	public void detectsSubmenus()
	{
		MenuEntry plain = new StubMenuEntry(MenuAction.CANCEL, -1, null);
		MenuEntry submenu = new StubMenuEntry(MenuAction.PLAYER_FIRST_OPTION, -1,
			new StubMenu(0, 0, 0, 0));

		assertFalse(MenuOverlay.containsSubMenu(new MenuEntry[]{plain}));
		assertTrue(MenuOverlay.containsSubMenu(new MenuEntry[]{plain, submenu}));
	}

	@Test
	public void classifiesEveryGameObjectActionBeforeItemIds()
	{
		MenuAction[] actions = {
			MenuAction.ITEM_USE_ON_GAME_OBJECT,
			MenuAction.WIDGET_TARGET_ON_GAME_OBJECT,
			MenuAction.GAME_OBJECT_FIRST_OPTION,
			MenuAction.GAME_OBJECT_SECOND_OPTION,
			MenuAction.GAME_OBJECT_THIRD_OPTION,
			MenuAction.GAME_OBJECT_FOURTH_OPTION,
			MenuAction.GAME_OBJECT_FIFTH_OPTION,
			MenuAction.EXAMINE_OBJECT
		};

		for (MenuAction action : actions)
		{
			MenuEntry entryWithItemId = new StubMenuEntry(action, 4151, null);
			assertEquals(action.name(), MenuRenderer.TargetKind.OBJECT, MenuOverlay.classify(entryWithItemId));
		}
	}

	private static final class StubMenu implements Menu
	{
		private final int x;
		private final int y;
		private final int width;
		private final int height;
		private MenuEntry[] entries = new MenuEntry[0];

		private StubMenu(int x, int y, int width, int height)
		{
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
		}

		@Override
		public MenuEntry createMenuEntry(int idx)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public MenuEntry[] getMenuEntries()
		{
			return entries;
		}

		@Override
		public void setMenuEntries(MenuEntry[] entries)
		{
			this.entries = entries;
		}

		@Override
		public void removeMenuEntry(MenuEntry entry)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public int getMenuX()
		{
			return x;
		}

		@Override
		public int getMenuY()
		{
			return y;
		}

		@Override
		public int getMenuWidth()
		{
			return width;
		}

		@Override
		public int getMenuHeight()
		{
			return height;
		}
	}

	private static final class StubMenuEntry implements MenuEntry
	{
		private MenuAction type;
		private int itemId;
		private final Menu submenu;

		private StubMenuEntry(MenuAction type, int itemId, Menu submenu)
		{
			this.type = type;
			this.itemId = itemId;
			this.submenu = submenu;
		}

		@Override
		public String getOption()
		{
			return "";
		}

		@Override
		public MenuEntry setOption(String option)
		{
			return this;
		}

		@Override
		public String getTarget()
		{
			return "";
		}

		@Override
		public MenuEntry setTarget(String target)
		{
			return this;
		}

		@Override
		public int getIdentifier()
		{
			return 0;
		}

		@Override
		public MenuEntry setIdentifier(int identifier)
		{
			return this;
		}

		@Override
		public MenuAction getType()
		{
			return type;
		}

		@Override
		public MenuEntry setType(MenuAction type)
		{
			this.type = type;
			return this;
		}

		@Override
		public int getParam0()
		{
			return 0;
		}

		@Override
		public MenuEntry setParam0(int param0)
		{
			return this;
		}

		@Override
		public int getParam1()
		{
			return 0;
		}

		@Override
		public MenuEntry setParam1(int param1)
		{
			return this;
		}

		@Override
		public boolean isForceLeftClick()
		{
			return false;
		}

		@Override
		public MenuEntry setForceLeftClick(boolean forceLeftClick)
		{
			return this;
		}

		@Override
		public int getWorldViewId()
		{
			return 0;
		}

		@Override
		public MenuEntry setWorldViewId(int worldViewId)
		{
			return this;
		}

		@Override
		public boolean isDeprioritized()
		{
			return false;
		}

		@Override
		public MenuEntry setDeprioritized(boolean deprioritized)
		{
			return this;
		}

		@Override
		public MenuEntry onClick(Consumer<MenuEntry> consumer)
		{
			return this;
		}

		@Override
		public Consumer<MenuEntry> onClick()
		{
			return null;
		}

		@Override
		public boolean isItemOp()
		{
			return false;
		}

		@Override
		public int getItemOp()
		{
			return 0;
		}

		@Override
		public int getItemId()
		{
			return itemId;
		}

		@Override
		public MenuEntry setItemId(int itemId)
		{
			this.itemId = itemId;
			return this;
		}

		@Override
		public Widget getWidget()
		{
			return null;
		}

		@Override
		public NPC getNpc()
		{
			return null;
		}

		@Override
		public Player getPlayer()
		{
			return null;
		}

		@Override
		public Actor getActor()
		{
			return null;
		}

		@Override
		public Menu getSubMenu()
		{
			return submenu;
		}

		@Override
		public Menu createSubMenu()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void deleteSubMenu()
		{
			throw new UnsupportedOperationException();
		}
	}
}
