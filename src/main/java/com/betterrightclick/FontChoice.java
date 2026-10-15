package com.betterrightclick;

import java.awt.Font;

/**
 * {@link #toString()} is overridden so {@code Text.titleCase} (used by combo box renderers)
 * shows these short labels instead of title-casing the RS_-prefixed constant names.
 */
enum FontChoice
{
	RS_REGULAR("Regular", null),
	RS_SMALL("Small", null),
	RS_BOLD("Bold", null),
	SANS("Sans", Font.SANS_SERIF),
	SERIF("Serif", Font.SERIF),
	MONO("Mono", Font.MONOSPACED);

	private final String label;
	// AWT logical family for the scalable fonts; null for RuneScape's fixed-size bitmap fonts.
	final String family;

	FontChoice(String label, String family)
	{
		this.label = label;
		this.family = family;
	}

	boolean isScalable()
	{
		return family != null;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
