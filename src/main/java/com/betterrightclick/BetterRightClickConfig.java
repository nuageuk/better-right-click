package com.betterrightclick;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

/**
 * Every item here is hidden; the sidebar panel is the only editor. Per-theme settings live on
 * {@link Theme}.
 */
@ConfigGroup(BetterRightClickConfig.GROUP)
public interface BetterRightClickConfig extends Config
{
	String GROUP = "betterrightclick";

	String KEY_USER_THEMES = "userThemes";
	String KEY_ACTIVE_THEME = "activeTheme";
	String KEY_HIGHLIGHTS_ENABLED = "highlightsEnabled";
	String KEY_HIGHLIGHTS_BOLD = "highlightsBold";
	String KEY_HIGHLIGHT_RULES = "highlightRules";
	String KEY_TARGET_MODE = "targetMode";
	String KEY_NPC_COLOUR = "npcColour";
	String KEY_PLAYER_COLOUR = "playerColour";
	String KEY_ITEM_COLOUR = "itemColour";
	String KEY_OBJECT_COLOUR = "objectColour";
	String KEY_OTHER_COLOUR = "otherColour";
	String KEY_COMBAT_LEVEL_MODE = "combatLevelMode";
	String KEY_LOWER_LEVEL_COLOUR = "lowerLevelColour";
	String KEY_SAME_LEVEL_COLOUR = "sameLevelColour";
	String KEY_HIGHER_LEVEL_COLOUR = "higherLevelColour";
	String KEY_TEXT_OFFSET_X = "textOffsetX";
	String KEY_BASELINE_NUDGE = "baselineNudge";
	String KEY_SHOW_HIT_ROWS = "showHitRows";

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

	@ConfigItem(keyName = KEY_HIGHLIGHTS_ENABLED, name = "", description = "", hidden = true)
	default boolean highlightsEnabled()
	{
		return true;
	}

	@ConfigItem(keyName = KEY_HIGHLIGHTS_BOLD, name = "", description = "", hidden = true)
	default boolean highlightsBold()
	{
		return true;
	}

	@ConfigItem(keyName = KEY_HIGHLIGHT_RULES, name = "", description = "", hidden = true)
	default String highlightRules()
	{
		return "Attack=#FF5C5C\nWalk here=#7D8590\nExamine=#7D8590";
	}

	// --- Targets ---

	@ConfigItem(keyName = KEY_TARGET_MODE, name = "", description = "", hidden = true)
	default TargetMode targetMode()
	{
		return TargetMode.GAME;
	}

	@ConfigItem(keyName = KEY_NPC_COLOUR, name = "", description = "", hidden = true)
	default Color npcColour()
	{
		return new Color(0xFFFF00);
	}

	@ConfigItem(keyName = KEY_PLAYER_COLOUR, name = "", description = "", hidden = true)
	default Color playerColour()
	{
		return new Color(0xFFFFFF);
	}

	@ConfigItem(keyName = KEY_ITEM_COLOUR, name = "", description = "", hidden = true)
	default Color itemColour()
	{
		return new Color(0xFF9040);
	}

	@ConfigItem(keyName = KEY_OBJECT_COLOUR, name = "", description = "", hidden = true)
	default Color objectColour()
	{
		return new Color(0x00FFFF);
	}

	@ConfigItem(keyName = KEY_OTHER_COLOUR, name = "", description = "", hidden = true)
	default Color otherColour()
	{
		return new Color(0xFFFFFF);
	}

	// --- Combat levels ---

	@ConfigItem(keyName = KEY_COMBAT_LEVEL_MODE, name = "", description = "", hidden = true)
	default CombatLevelMode combatLevelMode()
	{
		return CombatLevelMode.GAME;
	}

	@ConfigItem(keyName = KEY_LOWER_LEVEL_COLOUR, name = "", description = "", hidden = true)
	default Color lowerLevelColour()
	{
		return new Color(0x00FF00);
	}

	@ConfigItem(keyName = KEY_SAME_LEVEL_COLOUR, name = "", description = "", hidden = true)
	default Color sameLevelColour()
	{
		return new Color(0xFFFF00);
	}

	@ConfigItem(keyName = KEY_HIGHER_LEVEL_COLOUR, name = "", description = "", hidden = true)
	default Color higherLevelColour()
	{
		return new Color(0xFF3030);
	}

	// --- Advanced (geometry) ---

	@ConfigItem(keyName = KEY_TEXT_OFFSET_X, name = "", description = "", hidden = true)
	default int textOffsetX()
	{
		return 3;
	}

	@ConfigItem(keyName = KEY_BASELINE_NUDGE, name = "", description = "", hidden = true)
	default int baselineNudge()
	{
		return 0;
	}

	@ConfigItem(keyName = KEY_SHOW_HIT_ROWS, name = "", description = "", hidden = true)
	default boolean showHitRows()
	{
		return false;
	}
}
