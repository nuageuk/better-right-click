package com.betterrightclick;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Parses the persisted {@code option=#RRGGBB} rule lines. Shared by the panel's rule editor and the
 * renderer so both agree on what a valid rule is.
 */
final class HighlightRules
{
	private static final Pattern HEX = Pattern.compile("[0-9a-fA-F]{6}");

	private HighlightRules()
	{
	}

	static final class Rule
	{
		final String option;
		final Color colour;

		Rule(String option, Color colour)
		{
			this.option = option;
			this.colour = colour;
		}
	}

	/**
	 * Invalid lines are skipped; at most {@link Limits#MAX_RULES} rules are returned.
	 */
	static List<Rule> parse(String raw)
	{
		List<Rule> rules = new ArrayList<>();
		if (raw == null)
		{
			return rules;
		}
		for (String line : raw.split("\n"))
		{
			if (rules.size() >= Limits.MAX_RULES)
			{
				break;
			}
			int eq = line.lastIndexOf('=');
			if (eq <= 0 || eq == line.length() - 1)
			{
				continue;
			}
			String option = Limits.truncate(line.substring(0, eq).trim(), Limits.MAX_RULE_OPTION_LENGTH);
			String hex = line.substring(eq + 1).trim();
			if (hex.startsWith("#"))
			{
				hex = hex.substring(1);
			}
			if (option.isEmpty() || !HEX.matcher(hex).matches())
			{
				continue;
			}
			rules.add(new Rule(option, new Color(Integer.parseInt(hex, 16))));
		}
		return rules;
	}

	/**
	 * Immutable lookup keyed by lower-cased option text.
	 */
	static Map<String, Color> parseLookup(String raw)
	{
		Map<String, Color> lookup = new HashMap<>();
		for (Rule rule : parse(raw))
		{
			lookup.put(rule.option.toLowerCase(Locale.ROOT), rule.colour);
		}
		return Collections.unmodifiableMap(lookup);
	}
}
