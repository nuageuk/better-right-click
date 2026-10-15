package com.menuthemes;

import java.awt.Color;

/**
 * An immutable, fully self-contained menu style: colours, geometry style and visibility toggles.
 * Built-in themes are locked (never edited or deleted); user themes are editable and saved to config.
 */
final class Theme
{
	final String id;
	final String name;
	final boolean builtIn;
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
	final boolean showBorder;
	final boolean showShadow;
	final boolean showHeader;

	Theme(String id, String name, boolean builtIn, Color bg, Color border, Color header, Color headerText,
		Color text, Color hover, Color accent, int radius, int opacity, HoverStyle hoverStyle,
		boolean showBorder, boolean showShadow, boolean showHeader)
	{
		this.id = id;
		this.name = name;
		this.builtIn = builtIn;
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
		this.showBorder = showBorder;
		this.showShadow = showShadow;
		this.showHeader = showHeader;
	}

	Theme withIdentity(String id, String name, boolean builtIn)
	{
		return new Theme(id, name, builtIn, bg, border, header, headerText, text, hover, accent,
			radius, opacity, hoverStyle, showBorder, showShadow, showHeader);
	}

	Theme withBg(Color v)
	{
		return new Theme(id, name, builtIn, v, border, header, headerText, text, hover, accent,
			radius, opacity, hoverStyle, showBorder, showShadow, showHeader);
	}

	Theme withBorder(Color v)
	{
		return new Theme(id, name, builtIn, bg, v, header, headerText, text, hover, accent,
			radius, opacity, hoverStyle, showBorder, showShadow, showHeader);
	}

	Theme withHeader(Color v)
	{
		return new Theme(id, name, builtIn, bg, border, v, headerText, text, hover, accent,
			radius, opacity, hoverStyle, showBorder, showShadow, showHeader);
	}

	Theme withHeaderText(Color v)
	{
		return new Theme(id, name, builtIn, bg, border, header, v, text, hover, accent,
			radius, opacity, hoverStyle, showBorder, showShadow, showHeader);
	}

	Theme withText(Color v)
	{
		return new Theme(id, name, builtIn, bg, border, header, headerText, v, hover, accent,
			radius, opacity, hoverStyle, showBorder, showShadow, showHeader);
	}

	Theme withHover(Color v)
	{
		return new Theme(id, name, builtIn, bg, border, header, headerText, text, v, accent,
			radius, opacity, hoverStyle, showBorder, showShadow, showHeader);
	}

	Theme withAccent(Color v)
	{
		return new Theme(id, name, builtIn, bg, border, header, headerText, text, hover, v,
			radius, opacity, hoverStyle, showBorder, showShadow, showHeader);
	}

	Theme withRadius(int v)
	{
		return new Theme(id, name, builtIn, bg, border, header, headerText, text, hover, accent,
			v, opacity, hoverStyle, showBorder, showShadow, showHeader);
	}

	Theme withOpacity(int v)
	{
		return new Theme(id, name, builtIn, bg, border, header, headerText, text, hover, accent,
			radius, v, hoverStyle, showBorder, showShadow, showHeader);
	}

	Theme withHoverStyle(HoverStyle v)
	{
		return new Theme(id, name, builtIn, bg, border, header, headerText, text, hover, accent,
			radius, opacity, v, showBorder, showShadow, showHeader);
	}

	Theme withShowBorder(boolean v)
	{
		return new Theme(id, name, builtIn, bg, border, header, headerText, text, hover, accent,
			radius, opacity, hoverStyle, v, showShadow, showHeader);
	}

	Theme withShowShadow(boolean v)
	{
		return new Theme(id, name, builtIn, bg, border, header, headerText, text, hover, accent,
			radius, opacity, hoverStyle, showBorder, v, showHeader);
	}

	Theme withShowHeader(boolean v)
	{
		return new Theme(id, name, builtIn, bg, border, header, headerText, text, hover, accent,
			radius, opacity, hoverStyle, showBorder, showShadow, v);
	}
}
