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

	// Keys written by applyPreset() in MenuThemesPlugin; kept as constants to avoid typos.
	String KEY_THEME = "theme";
	String KEY_BG = "bgColour";
	String KEY_BORDER = "borderColour";
	String KEY_HEADER = "headerColour";
	String KEY_HEADER_TEXT = "headerTextColour";
	String KEY_TEXT = "textColour";
	String KEY_HOVER = "hoverColour";
	String KEY_ACCENT = "accentColour";
	String KEY_RADIUS = "cornerRadius";
	String KEY_OPACITY = "opacity";
	String KEY_HOVER_STYLE = "hoverStyle";

	@ConfigSection(
		name = "Theme",
		description = "Presets and overall style",
		position = 0
	)
	String themeSection = "themeSection";

	@ConfigSection(
		name = "Text",
		description = "Font settings",
		position = 10
	)
	String textSection = "textSection";

	@ConfigSection(
		name = "Colours",
		description = "Individual colours used to draw the menu. A preset above overwrites these",
		position = 20
	)
	String coloursSection = "coloursSection";

	@ConfigSection(
		name = "Hover",
		description = "Hover highlight style",
		position = 30
	)
	String hoverSection = "hoverSection";

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
		name = "Combat levels",
		description = "Colour the combat level shown in targets",
		position = 60
	)
	String levelsSection = "levelsSection";

	@ConfigSection(
		name = "Advanced",
		description = "Geometry and debugging",
		position = 70,
		closedByDefault = true
	)
	String advancedSection = "advancedSection";

	// --- Theme ---

	@ConfigItem(
		keyName = KEY_THEME,
		name = "Preset",
		description = "Applies a bundled theme. Choose Custom to keep your own colours",
		position = 1,
		section = themeSection
	)
	default Preset theme()
	{
		return Preset.DARK;
	}

	@Range(min = 40, max = 100)
	@ConfigItem(
		keyName = KEY_OPACITY,
		name = "Opacity",
		description = "Opacity of the background, header and hover fills",
		position = 2,
		section = themeSection
	)
	default int opacity()
	{
		return 96;
	}

	@Range(min = 0, max = 8)
	@ConfigItem(
		keyName = KEY_RADIUS,
		name = "Corner radius",
		description = "Corner radius of the menu",
		position = 3,
		section = themeSection
	)
	default int cornerRadius()
	{
		return 6;
	}

	@ConfigItem(
		keyName = "borderEnabled",
		name = "Border",
		description = "Draw a border around the menu",
		position = 4,
		section = themeSection
	)
	default boolean borderEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "dropShadowEnabled",
		name = "Drop shadow",
		description = "Draw a soft drop shadow behind the menu",
		position = 5,
		section = themeSection
	)
	default boolean dropShadowEnabled()
	{
		return false;
	}

	@ConfigItem(
		keyName = "headerEnabled",
		name = "Header",
		description = "Show the \"Choose Option\" header",
		position = 6,
		section = themeSection
	)
	default boolean headerEnabled()
	{
		return true;
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

	// --- Colours ---

	@ConfigItem(
		keyName = KEY_BG,
		name = "Background",
		description = "Background colour of the menu",
		position = 1,
		section = coloursSection
	)
	default Color bgColour()
	{
		return new Color(0x16181D);
	}

	@ConfigItem(
		keyName = KEY_BORDER,
		name = "Border",
		description = "Border colour of the menu",
		position = 2,
		section = coloursSection
	)
	default Color borderColour()
	{
		return new Color(0x2C313A);
	}

	@ConfigItem(
		keyName = KEY_HEADER,
		name = "Header background",
		description = "Background colour of the \"Choose Option\" header",
		position = 3,
		section = coloursSection
	)
	default Color headerColour()
	{
		return new Color(0x0F1115);
	}

	@ConfigItem(
		keyName = KEY_HEADER_TEXT,
		name = "Header text",
		description = "Text colour of the \"Choose Option\" header",
		position = 4,
		section = coloursSection
	)
	default Color headerTextColour()
	{
		return new Color(0x8A93A3);
	}

	@ConfigItem(
		keyName = KEY_TEXT,
		name = "Option text",
		description = "Text colour of menu entry options",
		position = 5,
		section = coloursSection
	)
	default Color textColour()
	{
		return new Color(0xE6E9EF);
	}

	@ConfigItem(
		keyName = KEY_HOVER,
		name = "Hover",
		description = "Colour used to highlight the hovered menu entry",
		position = 6,
		section = coloursSection
	)
	default Color hoverColour()
	{
		return new Color(0x2A3140);
	}

	@ConfigItem(
		keyName = KEY_ACCENT,
		name = "Accent",
		description = "Accent colour used by some hover styles and action highlights",
		position = 7,
		section = coloursSection
	)
	default Color accentColour()
	{
		return new Color(0x4C8DF6);
	}

	// --- Hover ---

	@ConfigItem(
		keyName = KEY_HOVER_STYLE,
		name = "Hover style",
		description = "How the hovered menu entry is highlighted",
		position = 1,
		section = hoverSection
	)
	default HoverStyle hoverStyle()
	{
		return HoverStyle.FILL;
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
		description = "Target colour for NPCs, when target mode is Custom",
		position = 2,
		section = targetsSection
	)
	default Color npcColour()
	{
		return new Color(0xFFFF00);
	}

	@ConfigItem(
		keyName = "playerColour",
		name = "Player",
		description = "Target colour for players, when target mode is Custom",
		position = 3,
		section = targetsSection
	)
	default Color playerColour()
	{
		return new Color(0xFFFFFF);
	}

	@ConfigItem(
		keyName = "itemColour",
		name = "Item",
		description = "Target colour for items, when target mode is Custom",
		position = 4,
		section = targetsSection
	)
	default Color itemColour()
	{
		return new Color(0xFF9040);
	}

	@ConfigItem(
		keyName = "objectColour",
		name = "Object",
		description = "Target colour for game objects, when target mode is Custom",
		position = 5,
		section = targetsSection
	)
	default Color objectColour()
	{
		return new Color(0x00FFFF);
	}

	@ConfigItem(
		keyName = "otherColour",
		name = "Other",
		description = "Target colour for anything else, when target mode is Custom",
		position = 6,
		section = targetsSection
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
		description = "Colour for a combat level lower than yours, when mode is Custom",
		position = 2,
		section = levelsSection
	)
	default Color lowerLevelColour()
	{
		return new Color(0x00FF00);
	}

	@ConfigItem(
		keyName = "sameLevelColour",
		name = "Same level",
		description = "Colour for a combat level equal to yours, when mode is Custom",
		position = 3,
		section = levelsSection
	)
	default Color sameLevelColour()
	{
		return new Color(0xFFFF00);
	}

	@ConfigItem(
		keyName = "higherLevelColour",
		name = "Higher level",
		description = "Colour for a combat level higher than yours, when mode is Custom",
		position = 4,
		section = levelsSection
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
