package com.menuthemes;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

/**
 * Every item here is hidden; the sidebar panel is the only editor. Theme-specific settings
 * (colours, geometry style, font, text shadow) live on {@link Theme} instead, not here.
 */
@ConfigGroup(MenuThemesConfig.GROUP)
public interface MenuThemesConfig extends Config
{
	String GROUP = "menuthemes";

	String KEY_USER_THEMES = "userThemes";
	String KEY_ACTIVE_THEME = "activeTheme";

	@ConfigItem(keyName = KEY_USER_THEMES, name = "", description = "", hidden = true)
	default String userThemes()
	{
		return "[]";
	}

	@ConfigItem(keyName = KEY_ACTIVE_THEME, name = "", description = "", hidden = true)
	default String activeTheme()
	{
		return BuiltInThemes.DEFAULT_ID;
	}

	// --- Action highlights ---

	@ConfigItem(keyName = "highlightsEnabled", name = "", description = "", hidden = true)
	default boolean highlightsEnabled()
	{
		return true;
	}

	@ConfigItem(keyName = "highlightsBold", name = "", description = "", hidden = true)
	default boolean highlightsBold()
	{
		return true;
	}

	@ConfigItem(keyName = "highlightRules", name = "", description = "", hidden = true)
	default String highlightRules()
	{
		return "Attack=#FF5C5C\nWalk here=#7D8590\nExamine=#7D8590";
	}

	// --- Targets ---

	@ConfigItem(keyName = "targetMode", name = "", description = "", hidden = true)
	default TargetMode targetMode()
	{
		return TargetMode.GAME;
	}

	@ConfigItem(keyName = "npcColour", name = "", description = "", hidden = true)
	default Color npcColour()
	{
		return new Color(0xFFFF00);
	}

	@ConfigItem(keyName = "playerColour", name = "", description = "", hidden = true)
	default Color playerColour()
	{
		return new Color(0xFFFFFF);
	}

	@ConfigItem(keyName = "itemColour", name = "", description = "", hidden = true)
	default Color itemColour()
	{
		return new Color(0xFF9040);
	}

	@ConfigItem(keyName = "objectColour", name = "", description = "", hidden = true)
	default Color objectColour()
	{
		return new Color(0x00FFFF);
	}

	@ConfigItem(keyName = "otherColour", name = "", description = "", hidden = true)
	default Color otherColour()
	{
		return new Color(0xFFFFFF);
	}

	// --- Combat levels ---

	@ConfigItem(keyName = "combatLevelMode", name = "", description = "", hidden = true)
	default CombatLevelMode combatLevelMode()
	{
		return CombatLevelMode.GAME;
	}

	@ConfigItem(keyName = "lowerLevelColour", name = "", description = "", hidden = true)
	default Color lowerLevelColour()
	{
		return new Color(0x00FF00);
	}

	@ConfigItem(keyName = "sameLevelColour", name = "", description = "", hidden = true)
	default Color sameLevelColour()
	{
		return new Color(0xFFFF00);
	}

	@ConfigItem(keyName = "higherLevelColour", name = "", description = "", hidden = true)
	default Color higherLevelColour()
	{
		return new Color(0xFF3030);
	}

	// --- Advanced (geometry) ---

	@ConfigItem(keyName = "headerHeight", name = "", description = "", hidden = true)
	default int headerHeight()
	{
		return 19;
	}

	@ConfigItem(keyName = "rowHeight", name = "", description = "", hidden = true)
	default int rowHeight()
	{
		return 15;
	}

	@ConfigItem(keyName = "textOffsetX", name = "", description = "", hidden = true)
	default int textOffsetX()
	{
		return 3;
	}

	@ConfigItem(keyName = "baselineNudge", name = "", description = "", hidden = true)
	default int baselineNudge()
	{
		return 0;
	}

	@ConfigItem(keyName = "showHitRows", name = "", description = "", hidden = true)
	default boolean showHitRows()
	{
		return false;
	}
}
