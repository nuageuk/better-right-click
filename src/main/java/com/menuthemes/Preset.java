package com.menuthemes;

import java.awt.Color;

/**
 * Bundled colour/style presets. {@link #CUSTOM} carries no values and is never applied.
 */
public enum Preset
{
	CUSTOM(null, null, null, null, null, null, null, 0, 100, null),
	DARK(
		new Color(0x16181D), new Color(0x2C313A), new Color(0x0F1115), new Color(0x8A93A3),
		new Color(0xE6E9EF), new Color(0x2A3140), new Color(0x4C8DF6), 6, 96, HoverStyle.FILL),
	MIDNIGHT(
		new Color(0x0E1530), new Color(0x26315C), new Color(0x0A1024), new Color(0x7F8CC0),
		new Color(0xE8ECFF), new Color(0x1E2A55), new Color(0x8EA2FF), 8, 92, HoverStyle.BAR),
	LIGHT(
		new Color(0xF7F6F2), new Color(0xD9D6CC), new Color(0xECEBE4), new Color(0x5F5B4E),
		new Color(0x22201B), new Color(0xE3E0D6), new Color(0x2F6FE0), 6, 100, HoverStyle.FILL),
	GLASS(
		new Color(0x0D1014), new Color(0x4A5562), new Color(0x0D1014), new Color(0x9AA6B5),
		new Color(0xF2F5F8), new Color(0x2B3440), new Color(0x9FE0FF), 8, 66, HoverStyle.OUTLINE),
	CLASSIC(
		new Color(0x5D5447), new Color(0x000000), new Color(0x000000), new Color(0x5D5447),
		new Color(0xFFFFFF), new Color(0x5D5447), new Color(0xFFFF00), 0, 100, HoverStyle.TEXT);

	final Color bg;
	final Color border;
	final Color header;
	final Color headerText;
	final Color text;
	final Color hover;
	final Color accent;
	final int radius;
	final int opacity;
	final HoverStyle hoverStyle;

	Preset(Color bg, Color border, Color header, Color headerText, Color text, Color hover,
		Color accent, int radius, int opacity, HoverStyle hoverStyle)
	{
		this.bg = bg;
		this.border = border;
		this.header = header;
		this.headerText = headerText;
		this.text = text;
		this.hover = hover;
		this.accent = accent;
		this.radius = radius;
		this.opacity = opacity;
		this.hoverStyle = hoverStyle;
	}
}
