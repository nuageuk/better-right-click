package com.betterrightclick;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

final class BuiltInThemes
{
	static final List<Theme> ALL = Collections.unmodifiableList(Arrays.asList(
		new Theme("dark", "Dark", true,
			new Color(0x16181D), new Color(0x2C313A), new Color(0x0F1115), new Color(0x8A93A3),
			new Color(0xE6E9EF), new Color(0x2A3140), new Color(0x4C8DF6), 6, 96, HoverStyle.FILL,
			true, false, true, FontChoice.RS_BOLD, 12, true),
		new Theme("midnight", "Midnight", true,
			new Color(0x0E1530), new Color(0x26315C), new Color(0x0A1024), new Color(0x7F8CC0),
			new Color(0xE8ECFF), new Color(0x1E2A55), new Color(0x8EA2FF), 8, 92, HoverStyle.BAR,
			true, false, true, FontChoice.RS_BOLD, 12, true),
		new Theme("light", "Light", true,
			new Color(0xF7F6F2), new Color(0xD9D6CC), new Color(0xECEBE4), new Color(0x5F5B4E),
			new Color(0x22201B), new Color(0xE3E0D6), new Color(0x2F6FE0), 6, 100, HoverStyle.FILL,
			true, false, true, FontChoice.RS_BOLD, 12, true),
		new Theme("glass", "Glass", true,
			new Color(0xB0B0B0), new Color(0x000000), new Color(0x000000), new Color(0xFFFFFF),
			new Color(0xFFFFFF), new Color(0xFFFFFF), new Color(0x120D0D), 8, 40, HoverStyle.FILL,
			true, false, true, FontChoice.RS_BOLD, 12, true),
		new Theme("classic", "Classic", true,
			new Color(0x5D5447), new Color(0x000000), new Color(0x000000), new Color(0x5D5447),
			new Color(0xFFFFFF), new Color(0x5D5447), new Color(0xFFFF00), 0, 100, HoverStyle.TEXT,
			true, false, true, FontChoice.RS_BOLD, 12, true)
	));

	static final String DEFAULT_ID = "classic";

	private BuiltInThemes()
	{
	}

	static Theme find(String id)
	{
		for (Theme theme : ALL)
		{
			if (theme.id.equals(id))
			{
				return theme;
			}
		}
		return null;
	}
}
