package com.menuthemes;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(MenuThemesConfig.GROUP)
public interface MenuThemesConfig extends Config
{
	String GROUP = "menuthemes";

	// Theme storage; written by ThemeManager. Edited from the sidebar panel, not the config UI.
	String KEY_USER_THEMES = "userThemes";
	String KEY_ACTIVE_THEME = "activeTheme";

	@ConfigSection(
		name = "Text",
		description = "Font settings",
		position = 10
	)
	String textSection = "textSection";

	@ConfigSection(
		name = "Action highlights",
		description = "Highlight specific menu options",
		position = 40
	)
	String highlightsSection = "highlightsSection";

	@ConfigSection(
		name = "Targets",
		description = "Colour menu targets by entity type",
		position = 50
	)
	String targetsSection = "targetsSection";

	@ConfigSection(
		name = "Target colours (Custom mode)",
		description = "Only used when Mode is Custom",
		position = 55,
		closedByDefault = true
	)
	String targetColoursSection = "targetColoursSection";

	@ConfigSection(
		name = "Combat levels",
		description = "Colour the combat level shown in targets",
		position = 60
	)
	String levelsSection = "levelsSection";

	@ConfigSection(
		name = "Level colours (Custom mode)",
		description = "Only used when Mode is Custom",
		position = 65,
		closedByDefault = true
	)
	String levelColoursSection = "levelColoursSection";

	@ConfigSection(
		name = "Advanced",
		description = "Geometry and debugging",
		position = 70,
		closedByDefault = true
	)
	String advancedSection = "advancedSection";

	@ConfigItem(
		keyName = KEY_USER_THEMES,
		name = "",
		description = "",
		hidden = true
	)
	default String userThemes()
	{
		return "[]";
	}

	@ConfigItem(
		keyName = KEY_ACTIVE_THEME,
		name = "",
		description = "",
		hidden = true
	)
	default String activeTheme()
	{
		return BuiltInThemes.DEFAULT_ID;
	}

	// --- Text ---

	@ConfigItem(
		keyName = "font",
		name = "Font",
		description = "Font used to draw the menu",
		position = 1,
		section = textSection
	)
	default FontChoice font()
	{
		return FontChoice.RS_REGULAR;
	}

	@Range(min = 11, max = 13)
	@ConfigItem(
		keyName = "fontSize",
		name = "Font size",
		description = "Font size, used only by the Sans and Mono fonts",
		position = 2,
		section = textSection
	)
	default int fontSize()
	{
		return 12;
	}

	@ConfigItem(
		keyName = "textShadow",
		name = "Text shadow",
		description = "Draw a 1px drop shadow behind all text",
		position = 3,
		section = textSection
	)
	default boolean textShadow()
	{
		return true;
	}

	// --- Action highlights ---

	@ConfigItem(
		keyName = "highlightsEnabled",
		name = "Enabled",
		description = "Highlight menu options matched by the rules below",
		position = 1,
		section = highlightsSection
	)
	default boolean highlightsEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightsBold",
		name = "Bold",
		description = "Draw highlighted options in a bold font",
		position = 2,
		section = highlightsSection
	)
	default boolean highlightsBold()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightRules",
		name = "Rules",
		description = "One rule per line: Option=#RRGGBB",
		position = 3,
		section = highlightsSection
	)
	default String highlightRules()
	{
		return "Attack=#FF5C5C\nWalk here=#7D8590\nExamine=#7D8590";
	}

	// --- Targets ---

	@ConfigItem(
		keyName = "targetMode",
		name = "Mode",
		description = "Game uses the game's own <col> colours, Custom uses the colours below",
		position = 1,
		section = targetsSection
	)
	default TargetMode targetMode()
	{
		return TargetMode.GAME;
	}

	@ConfigItem(
		keyName = "npcColour",
		name = "NPC",
		description = "Target colour for NPCs",
		position = 1,
		section = targetColoursSection
	)
	default Color npcColour()
	{
		return new Color(0xFFFF00);
	}

	@ConfigItem(
		keyName = "playerColour",
		name = "Player",
		description = "Target colour for players",
		position = 2,
		section = targetColoursSection
	)
	default Color playerColour()
	{
		return new Color(0xFFFFFF);
	}

	@ConfigItem(
		keyName = "itemColour",
		name = "Item",
		description = "Target colour for items",
		position = 3,
		section = targetColoursSection
	)
	default Color itemColour()
	{
		return new Color(0xFF9040);
	}

	@ConfigItem(
		keyName = "objectColour",
		name = "Object",
		description = "Target colour for game objects",
		position = 4,
		section = targetColoursSection
	)
	default Color objectColour()
	{
		return new Color(0x00FFFF);
	}

	@ConfigItem(
		keyName = "otherColour",
		name = "Other",
		description = "Target colour for anything else",
		position = 5,
		section = targetColoursSection
	)
	default Color otherColour()
	{
		return new Color(0xFFFFFF);
	}

	// --- Combat levels ---

	@ConfigItem(
		keyName = "combatLevelMode",
		name = "Mode",
		description = "Game keeps the game's own colours, Custom compares against your level, Hide removes it",
		position = 1,
		section = levelsSection
	)
	default CombatLevelMode combatLevelMode()
	{
		return CombatLevelMode.GAME;
	}

	@ConfigItem(
		keyName = "lowerLevelColour",
		name = "Lower level",
		description = "Colour for a combat level lower than yours",
		position = 1,
		section = levelColoursSection
	)
	default Color lowerLevelColour()
	{
		return new Color(0x00FF00);
	}

	@ConfigItem(
		keyName = "sameLevelColour",
		name = "Same level",
		description = "Colour for a combat level equal to yours",
		position = 2,
		section = levelColoursSection
	)
	default Color sameLevelColour()
	{
		return new Color(0xFFFF00);
	}

	@ConfigItem(
		keyName = "higherLevelColour",
		name = "Higher level",
		description = "Colour for a combat level higher than yours",
		position = 3,
		section = levelColoursSection
	)
	default Color higherLevelColour()
	{
		return new Color(0xFF3030);
	}

	// --- Advanced ---

	@ConfigItem(
		keyName = "headerHeight",
		name = "Header height",
		description = "Height in pixels of the \"Choose Option\" header",
		position = 1,
		section = advancedSection
	)
	default int headerHeight()
	{
		return 19;
	}

	@ConfigItem(
		keyName = "rowHeight",
		name = "Row height",
		description = "Height in pixels of each menu entry row",
		position = 2,
		section = advancedSection
	)
	default int rowHeight()
	{
		return 15;
	}

	@ConfigItem(
		keyName = "textOffsetX",
		name = "Text offset X",
		description = "Horizontal offset in pixels of text from the left edge",
		position = 3,
		section = advancedSection
	)
	default int textOffsetX()
	{
		return 3;
	}

	@ConfigItem(
		keyName = "baselineNudge",
		name = "Baseline nudge",
		description = "Fine-tune vertical text position, in pixels, after automatic centring",
		position = 4,
		section = advancedSection
	)
	default int baselineNudge()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "showHitRows",
		name = "Show hit rows",
		description = "Outline each computed row in magenta, for tuning the geometry",
		position = 5,
		section = advancedSection
	)
	default boolean showHitRows()
	{
		return false;
	}
}
