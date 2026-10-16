package com.rightclickstyler;

/**
 * Valid ranges for every user-editable value. Shared by the panel's spinners and by everything that
 * loads values from config or an import string, so out-of-range data can never reach the renderer.
 */
final class Limits
{
	static final int MIN_RADIUS = 0;
	static final int MAX_RADIUS = 8;
	static final int MIN_OPACITY = 40;
	static final int MAX_OPACITY = 100;
	static final int MIN_FONT_SIZE = 11;
	static final int MAX_FONT_SIZE = 13;

	static final int MIN_TEXT_OFFSET_X = 0;
	static final int MAX_TEXT_OFFSET_X = 20;
	static final int MIN_BASELINE_NUDGE = -10;
	static final int MAX_BASELINE_NUDGE = 10;

	static final int MAX_THEME_NAME_LENGTH = 40;
	static final int MAX_THEME_ID_LENGTH = 64;
	static final int MAX_USER_THEMES = 50;
	static final int MAX_IMPORT_LENGTH = 4096;
	static final int MAX_RULES = 50;
	static final int MAX_RULE_OPTION_LENGTH = 64;

	private Limits()
	{
	}

	static int clamp(int value, int min, int max)
	{
		return Math.max(min, Math.min(max, value));
	}

	static String truncate(String value, int maxLength)
	{
		if (value.length() <= maxLength)
		{
			return value;
		}
		int end = maxLength;
		if (end > 0 && Character.isHighSurrogate(value.charAt(end - 1))
			&& Character.isLowSurrogate(value.charAt(end)))
		{
			end--;
		}
		return value.substring(0, end);
	}

	static int textOffsetX(RightClickStylerConfig config)
	{
		return clamp(config.textOffsetX(), MIN_TEXT_OFFSET_X, MAX_TEXT_OFFSET_X);
	}

	static int baselineNudge(RightClickStylerConfig config)
	{
		return clamp(config.baselineNudge(), MIN_BASELINE_NUDGE, MAX_BASELINE_NUDGE);
	}
}
