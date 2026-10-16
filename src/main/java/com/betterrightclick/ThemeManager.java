package com.betterrightclick;

import com.google.gson.Gson;
import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

/**
 * Owns the built-in and user themes, which theme is active, and the "working copy" that the overlay
 * and the sidebar preview both render live as the user edits. User themes are saved as JSON in
 * the hidden {@code userThemes}/{@code activeTheme} config keys.
 * <p>
 * Everything except {@link #effectiveTheme()} is only touched from the EDT (the panel). The overlay
 * reads {@link #effectiveTheme()} on the client thread; {@link Theme} is immutable and the reference
 * is volatile, so it always sees a complete theme.
 */
@Slf4j
@Singleton
class ThemeManager
{
	private static final String EXPORT_PREFIX = "betterrightclick:";
	private static final String DEFAULT_THEME_NAME = "Theme";
	private static final String COPY_SUFFIX = " copy";

	static final String ERROR_FULL = "You already have the maximum of " + Limits.MAX_USER_THEMES + " themes.";
	private static final String ERROR_TOO_LONG = "That text is too long to be an exported theme.";
	private static final String ERROR_INVALID = "That doesn't look like a valid exported theme.";

	private final ConfigManager configManager;
	private final BetterRightClickConfig config;
	private final Gson gson;

	private final Map<String, Theme> userThemes = new LinkedHashMap<>();
	private String activeId = BuiltInThemes.DEFAULT_ID;
	private volatile Theme working = BuiltInThemes.find(BuiltInThemes.DEFAULT_ID);

	@Inject
	ThemeManager(ConfigManager configManager, BetterRightClickConfig config, Gson gson)
	{
		this.configManager = configManager;
		this.config = config;
		this.gson = gson;
		reload();
	}

	Theme effectiveTheme()
	{
		return working;
	}

	void setWorking(Theme theme)
	{
		working = theme;
	}

	List<Theme> builtIns()
	{
		return BuiltInThemes.ALL;
	}

	List<Theme> userThemes()
	{
		return new ArrayList<>(userThemes.values());
	}

	String activeId()
	{
		return activeId;
	}

	Theme active()
	{
		Theme theme = find(activeId);
		return theme != null ? theme : BuiltInThemes.find(BuiltInThemes.DEFAULT_ID);
	}

	boolean isEdited()
	{
		return !working.sameStyle(active());
	}

	private boolean isFull()
	{
		return userThemes.size() >= Limits.MAX_USER_THEMES;
	}

	void applyTheme(String id)
	{
		Theme theme = find(id);
		if (theme == null)
		{
			return;
		}
		activeId = id;
		working = theme;
		persistActiveId();
	}

	void save()
	{
		Theme active = userThemes.get(activeId);
		if (active == null)
		{
			return;
		}
		Theme saved = working.withIdentity(active.id, active.name, false);
		userThemes.put(saved.id, saved);
		working = saved;
		persistUserThemes();
	}

	/**
	 * @return false if the user theme limit has been reached
	 */
	boolean saveAsNew(String name)
	{
		return addAndActivate(working, name);
	}

	void revert()
	{
		working = active();
	}

	void rename(String newName)
	{
		Theme active = userThemes.get(activeId);
		if (active == null)
		{
			return;
		}
		Theme renamed = active.withIdentity(active.id, cleanName(newName), false);
		userThemes.put(renamed.id, renamed);
		if (working.id.equals(active.id))
		{
			working = working.withIdentity(renamed.id, renamed.name, false);
		}
		persistUserThemes();
	}

	/**
	 * @return false if the user theme limit has been reached
	 */
	boolean duplicate()
	{
		Theme source = active();
		return addAndActivate(source, source.name + COPY_SUFFIX);
	}

	void delete(String id)
	{
		if (userThemes.remove(id) == null)
		{
			return;
		}
		persistUserThemes();
		if (activeId.equals(id))
		{
			applyTheme(BuiltInThemes.DEFAULT_ID);
		}
	}

	String export(String id)
	{
		Theme theme = find(id);
		if (theme == null)
		{
			return null;
		}
		String json = gson.toJson(StoredTheme.from(theme));
		return EXPORT_PREFIX + Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Never throws.
	 *
	 * @return null on success, otherwise a message suitable for showing to the user
	 */
	String importTheme(String payload)
	{
		if (isFull())
		{
			return ERROR_FULL;
		}
		String trimmed = payload == null ? "" : payload.trim();
		if (trimmed.length() > Limits.MAX_IMPORT_LENGTH)
		{
			return ERROR_TOO_LONG;
		}
		if (trimmed.startsWith(EXPORT_PREFIX))
		{
			trimmed = trimmed.substring(EXPORT_PREFIX.length());
		}

		Theme theme;
		try
		{
			String json = new String(Base64.getDecoder().decode(trimmed), StandardCharsets.UTF_8);
			StoredTheme stored = gson.fromJson(json, StoredTheme.class);
			// an exported theme always carries an id; requiring one rejects arbitrary JSON objects
			theme = stored == null || stored.id == null || stored.id.isEmpty()
				? null : stored.toTheme(UUID.randomUUID().toString());
		}
		catch (RuntimeException e)
		{
			log.debug("Rejected theme import", e);
			return ERROR_INVALID;
		}
		if (theme == null)
		{
			return ERROR_INVALID;
		}

		addAndActivate(theme, theme.name);
		return null;
	}

	/**
	 * Replaces cached theme state from the active RuneLite configuration profile. This runs during
	 * construction and again after profile changes. Corrupt config loads as defaults rather than
	 * failing startup.
	 */
	void reload()
	{
		userThemes.clear();
		try
		{
			StoredTheme[] stored = gson.fromJson(config.userThemes(), StoredTheme[].class);
			if (stored != null)
			{
				for (StoredTheme s : stored)
				{
					if (userThemes.size() >= Limits.MAX_USER_THEMES)
					{
						break;
					}
					// first occurrence of a duplicated id wins
					if (s != null && isValidStoredId(s.id) && !userThemes.containsKey(s.id))
					{
						userThemes.put(s.id, s.toTheme(s.id));
					}
				}
			}
		}
		catch (RuntimeException e)
		{
			log.warn("Saved Better Right Click themes are corrupt; starting with none", e);
			userThemes.clear();
		}

		activeId = config.activeTheme();
		Theme theme = find(activeId);
		if (theme == null)
		{
			activeId = BuiltInThemes.DEFAULT_ID;
			theme = BuiltInThemes.find(activeId);
		}
		working = theme;
	}

	private static boolean isValidStoredId(String id)
	{
		return id != null && !id.isEmpty() && id.length() <= Limits.MAX_THEME_ID_LENGTH
			&& BuiltInThemes.find(id) == null;
	}

	private boolean addAndActivate(Theme source, String name)
	{
		if (isFull())
		{
			return false;
		}
		String id = UUID.randomUUID().toString();
		Theme theme = source.withIdentity(id, cleanName(name), false);
		userThemes.put(id, theme);
		activeId = id;
		working = theme;
		persistUserThemes();
		persistActiveId();
		return true;
	}

	private Theme find(String id)
	{
		if (id == null)
		{
			return null;
		}
		Theme builtIn = BuiltInThemes.find(id);
		return builtIn != null ? builtIn : userThemes.get(id);
	}

	private static String cleanName(String name)
	{
		String value = name == null ? "" : name;
		StringBuilder cleaned = null;
		for (int offset = 0; offset < value.length();)
		{
			int codePoint = value.codePointAt(offset);
			int width = Character.charCount(codePoint);
			if (Character.isISOControl(codePoint))
			{
				if (cleaned == null)
				{
					cleaned = new StringBuilder(value.length());
					cleaned.append(value, 0, offset);
				}
			}
			else if (cleaned != null)
			{
				cleaned.appendCodePoint(codePoint);
			}
			offset += width;
		}

		String trimmed = (cleaned == null ? value : cleaned.toString()).trim();
		return trimmed.isEmpty() ? DEFAULT_THEME_NAME : Limits.truncate(trimmed, Limits.MAX_THEME_NAME_LENGTH);
	}

	private void persistUserThemes()
	{
		List<StoredTheme> stored = new ArrayList<>(userThemes.size());
		for (Theme theme : userThemes.values())
		{
			stored.add(StoredTheme.from(theme));
		}
		configManager.setConfiguration(BetterRightClickConfig.GROUP, BetterRightClickConfig.KEY_USER_THEMES, gson.toJson(stored));
	}

	private void persistActiveId()
	{
		configManager.setConfiguration(BetterRightClickConfig.GROUP, BetterRightClickConfig.KEY_ACTIVE_THEME, activeId);
	}

	/**
	 * Gson-friendly shape for a theme: colours as plain ARGB ints, enums as names. Used for both the
	 * persisted user theme list and the clipboard export/import format. Boxed fields so that values
	 * missing from the JSON can be told apart and defaulted from the default built-in theme.
	 */
	private static final class StoredTheme
	{
		String id;
		String name;
		Integer bg;
		Integer border;
		Integer header;
		Integer headerText;
		Integer text;
		Integer hover;
		Integer accent;
		Integer radius;
		Integer opacity;
		String hoverStyle;
		Boolean showBorder;
		Boolean showShadow;
		Boolean showHeader;
		String font;
		Integer fontSize;
		Boolean textShadow;

		static StoredTheme from(Theme t)
		{
			StoredTheme s = new StoredTheme();
			s.id = t.id;
			s.name = t.name;
			s.bg = t.bg.getRGB();
			s.border = t.border.getRGB();
			s.header = t.header.getRGB();
			s.headerText = t.headerText.getRGB();
			s.text = t.text.getRGB();
			s.hover = t.hover.getRGB();
			s.accent = t.accent.getRGB();
			s.radius = t.radius;
			s.opacity = t.opacity;
			s.hoverStyle = t.hoverStyle.name();
			s.showBorder = t.showBorder;
			s.showShadow = t.showShadow;
			s.showHeader = t.showHeader;
			s.font = t.font.name();
			s.fontSize = t.fontSize;
			s.textShadow = t.textShadow;
			return s;
		}

		Theme toTheme(String themeId)
		{
			Theme d = BuiltInThemes.find(BuiltInThemes.DEFAULT_ID);
			return new Theme(themeId, cleanName(name), false,
				colour(bg, d.bg), colour(border, d.border), colour(header, d.header),
				colour(headerText, d.headerText), colour(text, d.text), colour(hover, d.hover),
				colour(accent, d.accent),
				Limits.clamp(radius != null ? radius : d.radius, Limits.MIN_RADIUS, Limits.MAX_RADIUS),
				Limits.clamp(opacity != null ? opacity : d.opacity, Limits.MIN_OPACITY, Limits.MAX_OPACITY),
				enumOr(HoverStyle.class, hoverStyle, d.hoverStyle),
				showBorder != null ? showBorder : d.showBorder,
				showShadow != null ? showShadow : d.showShadow,
				showHeader != null ? showHeader : d.showHeader,
				enumOr(FontChoice.class, font, d.font),
				Limits.clamp(fontSize != null ? fontSize : d.fontSize, Limits.MIN_FONT_SIZE, Limits.MAX_FONT_SIZE),
				textShadow != null ? textShadow : d.textShadow);
		}

		private static Color colour(Integer argb, Color fallback)
		{
			return argb != null ? new Color(argb, true) : fallback;
		}

		private static <E extends Enum<E>> E enumOr(Class<E> type, String name, E fallback)
		{
			if (name == null)
			{
				return fallback;
			}
			try
			{
				return Enum.valueOf(type, name);
			}
			catch (IllegalArgumentException e)
			{
				return fallback;
			}
		}
	}
}
