package com.betterrightclick;

/**
 * {@link #toString()} is overridden so {@code Text.titleCase} (used by combo box renderers)
 * shows these short labels instead of title-casing the RS_-prefixed constant names.
 */
enum FontChoice
{
	RS_REGULAR("Regular"),
	RS_SMALL("Small"),
	RS_BOLD("Bold"),
	SANS("Sans"),
	MONO("Mono");

	private final String label;

	FontChoice(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
