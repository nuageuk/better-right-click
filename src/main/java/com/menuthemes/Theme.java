package com.menuthemes;

import java.awt.Color;

/**
 * Resolved colour/style values for the current preset, or the custom keys when the preset is {@link Preset#CUSTOM}.
 */
final class Theme
{
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

	private Theme(Color bg, Color border, Color header, Color headerText, Color text, Color hover,
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

	static Theme of(MenuThemesConfig config)
	{
		Preset preset = config.theme();
		if (preset == Preset.CUSTOM)
		{
			return new Theme(config.bgColour(), config.borderColour(), config.headerColour(), config.headerTextColour(),
				config.textColour(), config.hoverColour(), config.accentColour(), config.cornerRadius(), config.opacity(),
				config.hoverStyle());
		}
		return new Theme(preset.bg, preset.border, preset.header, preset.headerText, preset.text, preset.hover,
			preset.accent, preset.radius, preset.opacity, preset.hoverStyle);
	}
}
