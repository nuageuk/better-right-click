package com.menuthemes;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
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
import net.runelite.client.config.ConfigManager;

/**
 * Owns the built-in and user themes, which theme is active, and the "working copy" that the overlay
 * and the sidebar preview both render live as the user edits. User themes are persisted as JSON
 * (via the injected {@link Gson}) in the hidden {@code userThemes}/{@code activeTheme} config keys.
 */
@Singleton
class ThemeManager
{
	private static final String EXPORT_PREFIX = "menuthemes:";

	private final ConfigManager configManager;
	private final MenuThemesConfig config;
	private final Gson gson;

	private final Map<String, Theme> userThemes = new LinkedHashMap<>();
	private String activeId = BuiltInThemes.DEFAULT_ID;

	/**
	 * The theme actually drawn right now. Read by the overlay on the client thread and by the
	 * sidebar preview on the EDT; written only from the EDT (the panel).
	 */
	private volatile Theme working = BuiltInThemes.find(BuiltInThemes.DEFAULT_ID);

	@Inject
	ThemeManager(ConfigManager configManager, MenuThemesConfig config, Gson gson)
	{
		this.configManager = configManager;
		this.config = config;
		this.gson = gson;
		load();
	}

	/**
	 * Loads saved state. Called once, from the constructor, so that every consumer Guice hands this
	 * singleton to - including the sidebar panel, which reads it while building its own UI - sees
	 * the real saved state rather than the hardcoded Dark-theme defaults.
	 */
	private void load()
	{
		userThemes.clear();
		try
		{
			List<StoredTheme> stored = gson.fromJson(config.userThemes(), new TypeToken<List<StoredTheme>>()
			{
			}.getType());
			if (stored != null)
			{
				for (StoredTheme s : stored)
				{
					Theme theme = s.toTheme();
					if (theme != null)
					{
						userThemes.put(theme.id, theme);
					}
				}
			}
		}
		catch (JsonSyntaxException e)
		{
			// corrupt config value; start with no user themes rather than failing to load
		}

		activeId = config.activeTheme();
		working = find(activeId);
		if (working == null)
		{
			activeId = BuiltInThemes.DEFAULT_ID;
			working = BuiltInThemes.find(activeId);
		}
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
		return !sameStyle(working, active());
	}

	private Theme find(String id)
	{
		Theme builtIn = BuiltInThemes.find(id);
		return builtIn != null ? builtIn : userThemes.get(id);
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
		configManager.setConfiguration(MenuThemesConfig.GROUP, MenuThemesConfig.KEY_ACTIVE_THEME, activeId);
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

	Theme saveAsNew(String name)
	{
		String id = UUID.randomUUID().toString();
		Theme theme = working.withIdentity(id, name, false);
		userThemes.put(id, theme);
		activeId = id;
		working = theme;
		persistUserThemes();
		persistActiveId();
		return theme;
	}

	void revert()
	{
		working = active();
	}

	boolean rename(String newName)
	{
		Theme active = userThemes.get(activeId);
		if (active == null)
		{
			return false;
		}
		Theme renamed = active.withIdentity(active.id, newName, false);
		userThemes.put(renamed.id, renamed);
		if (working.id.equals(active.id))
		{
			working = working.withIdentity(renamed.id, renamed.name, false);
		}
		persistUserThemes();
		return true;
	}

	Theme duplicate()
	{
		Theme source = active();
		String id = UUID.randomUUID().toString();
		Theme copy = source.withIdentity(id, source.name + " copy", false);
		userThemes.put(id, copy);
		activeId = id;
		working = copy;
		persistUserThemes();
		persistActiveId();
		return copy;
	}

	boolean delete(String id)
	{
		Theme removed = userThemes.remove(id);
		if (removed == null)
		{
			return false;
		}
		persistUserThemes();
		if (activeId.equals(id))
		{
			applyTheme(BuiltInThemes.DEFAULT_ID);
		}
		return true;
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
	 * @throws IllegalArgumentException if the payload isn't a valid exported theme
	 */
	Theme importTheme(String payload)
	{
		String trimmed = payload.trim();
		if (trimmed.startsWith(EXPORT_PREFIX))
		{
			trimmed = trimmed.substring(EXPORT_PREFIX.length());
		}

		String json;
		try
		{
			json = new String(Base64.getDecoder().decode(trimmed), StandardCharsets.UTF_8);
		}
		catch (IllegalArgumentException e)
		{
			throw new IllegalArgumentException("Not valid base64", e);
		}

		StoredTheme stored;
		try
		{
			stored = gson.fromJson(json, StoredTheme.class);
		}
		catch (JsonSyntaxException e)
		{
			throw new IllegalArgumentException("Not a valid theme", e);
		}

		Theme theme = stored == null ? null : stored.toTheme();
		if (theme == null)
		{
			throw new IllegalArgumentException("Not a valid theme");
		}

		String id = UUID.randomUUID().toString();
		Theme imported = theme.withIdentity(id, theme.name, false);
		userThemes.put(id, imported);
		activeId = id;
		working = imported;
		persistUserThemes();
		persistActiveId();
		return imported;
	}

	private static boolean sameStyle(Theme a, Theme b)
	{
		return a.bg.equals(b.bg) && a.border.equals(b.border) && a.header.equals(b.header)
			&& a.headerText.equals(b.headerText) && a.text.equals(b.text) && a.hover.equals(b.hover)
			&& a.accent.equals(b.accent) && a.radius == b.radius && a.opacity == b.opacity
			&& a.hoverStyle == b.hoverStyle && a.showBorder == b.showBorder && a.showShadow == b.showShadow
			&& a.showHeader == b.showHeader && a.font == b.font && a.fontSize == b.fontSize
			&& a.textShadow == b.textShadow;
	}

	private void persistUserThemes()
	{
		List<StoredTheme> stored = new ArrayList<>(userThemes.size());
		for (Theme theme : userThemes.values())
		{
			stored.add(StoredTheme.from(theme));
		}
		configManager.setConfiguration(MenuThemesConfig.GROUP, MenuThemesConfig.KEY_USER_THEMES, gson.toJson(stored));
	}

	private void persistActiveId()
	{
		configManager.setConfiguration(MenuThemesConfig.GROUP, MenuThemesConfig.KEY_ACTIVE_THEME, activeId);
	}

	/**
	 * Gson-friendly shape for a theme: colours as plain RGB ints, enum as its name. Used for both
	 * the persisted user theme list and the clipboard export/import format.
	 */
	private static final class StoredTheme
	{
		String id;
		String name;
		int bg;
		int border;
		int header;
		int headerText;
		int text;
		int hover;
		int accent;
		int radius;
		int opacity;
		String hoverStyle;
		boolean showBorder;
		boolean showShadow;
		boolean showHeader;
		String font;
		int fontSize;
		boolean textShadow;

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

		Theme toTheme()
		{
			if (id == null || id.isEmpty())
			{
				return null;
			}
			HoverStyle style;
			try
			{
				style = HoverStyle.valueOf(hoverStyle);
			}
			catch (IllegalArgumentException | NullPointerException e)
			{
				style = HoverStyle.FILL;
			}
			FontChoice fontChoice;
			try
			{
				fontChoice = FontChoice.valueOf(font);
			}
			catch (IllegalArgumentException | NullPointerException e)
			{
				fontChoice = FontChoice.RS_REGULAR;
			}
			return new Theme(id, name == null || name.isEmpty() ? "Theme" : name, false,
				new Color(bg, true), new Color(border, true), new Color(header, true), new Color(headerText, true),
				new Color(text, true), new Color(hover, true), new Color(accent, true),
				clamp(radius, 0, 8), clamp(opacity, 40, 100), style, showBorder, showShadow, showHeader,
				fontChoice, clamp(fontSize, 11, 13), textShadow);
		}

		private static int clamp(int value, int min, int max)
		{
			return Math.max(min, Math.min(max, value));
		}
	}
}
