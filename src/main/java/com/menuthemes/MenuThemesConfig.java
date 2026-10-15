package com.menuthemes;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("menuthemes")
public interface MenuThemesConfig extends Config
{
	@ConfigItem(
		keyName = "bgColour",
		name = "Background",
		description = "Background colour of the menu",
		position = 1
	)
	default Color bgColour()
	{
		return new Color(0x16181D);
	}

	@ConfigItem(
		keyName = "borderColour",
		name = "Border",
		description = "Border colour of the menu",
		position = 2
	)
	default Color borderColour()
	{
		return new Color(0x2C313A);
	}

	@ConfigItem(
		keyName = "headerColour",
		name = "Header background",
		description = "Background colour of the \"Choose Option\" header",
		position = 3
	)
	default Color headerColour()
	{
		return new Color(0x0F1115);
	}

	@ConfigItem(
		keyName = "headerTextColour",
		name = "Header text",
		description = "Text colour of the \"Choose Option\" header",
		position = 4
	)
	default Color headerTextColour()
	{
		return new Color(0x8A93A3);
	}

	@ConfigItem(
		keyName = "textColour",
		name = "Option text",
		description = "Text colour of menu entry options",
		position = 5
	)
	default Color textColour()
	{
		return new Color(0xE6E9EF);
	}

	@ConfigItem(
		keyName = "hoverColour",
		name = "Hover",
		description = "Fill colour of the hovered menu entry",
		position = 6
	)
	default Color hoverColour()
	{
		return new Color(0x2A3140);
	}

	@ConfigItem(
		keyName = "headerHeight",
		name = "Header height",
		description = "Height in pixels of the \"Choose Option\" header",
		position = 7
	)
	default int headerHeight()
	{
		return 19;
	}

	@ConfigItem(
		keyName = "rowHeight",
		name = "Row height",
		description = "Height in pixels of each menu entry row",
		position = 8
	)
	default int rowHeight()
	{
		return 15;
	}

	@ConfigItem(
		keyName = "textOffsetX",
		name = "Text offset X",
		description = "Horizontal offset in pixels of row text from the left edge",
		position = 9
	)
	default int textOffsetX()
	{
		return 3;
	}

	@ConfigItem(
		keyName = "textBaselineOffset",
		name = "Text baseline offset",
		description = "Vertical offset in pixels of row text from the top of the row",
		position = 10
	)
	default int textBaselineOffset()
	{
		return 12;
	}

	@ConfigItem(
		keyName = "showHitRows",
		name = "Show hit rows",
		description = "Outline each computed row in magenta, for tuning the geometry",
		position = 11
	)
	default boolean showHitRows()
	{
		return false;
	}
}
