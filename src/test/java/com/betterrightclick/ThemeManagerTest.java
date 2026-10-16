package com.betterrightclick;

import com.google.gson.Gson;
import java.awt.Color;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ThemeManagerTest
{
	@Test
	public void rejectsMalformedAndOversizedImports()
	{
		TestConfig config = new TestConfig();
		ThemeManager manager = manager(config);

		assertNotNull(manager.importTheme("not base64"));
		assertNotNull(manager.importTheme("x".repeat(Limits.MAX_IMPORT_LENGTH + 1)));
		assertTrue(manager.userThemes().isEmpty());
		assertEquals(0, config.writes);
	}

	@Test
	public void exportsUnsavedWorkingEditsWithoutOverwritingBuiltIns()
	{
		TestConfig sourceConfig = new TestConfig();
		ThemeManager source = manager(sourceConfig);
		Color editedBackground = new Color(0x123456);
		source.setWorking(source.effectiveTheme().withBg(editedBackground));

		String payload = source.exportWorking();

		assertEquals(0, sourceConfig.writes);
		TestConfig targetConfig = new TestConfig();
		ThemeManager target = manager(targetConfig);
		assertNull(target.importTheme(payload));
		assertNotEquals(BuiltInThemes.DEFAULT_ID, target.activeId());
		assertFalse(target.active().builtIn);
		assertEquals(editedBackground, target.active().bg);
		assertTrue(BuiltInThemes.find(BuiltInThemes.DEFAULT_ID).builtIn);
		assertNotEquals(editedBackground, BuiltInThemes.find(BuiltInThemes.DEFAULT_ID).bg);
	}

	@Test
	public void reloadReplacesThemeStateWithoutWritingStaleEdits()
	{
		TestConfig config = new TestConfig();
		ThemeManager manager = manager(config);
		assertTrue(manager.saveAsNew("Profile one"));
		manager.setWorking(manager.effectiveTheme().withOpacity(51));
		int writesBeforeReload = config.writes;

		config.userThemes = "[]";
		config.activeTheme = BuiltInThemes.DEFAULT_ID;
		manager.reload();

		assertEquals(writesBeforeReload, config.writes);
		assertEquals(BuiltInThemes.DEFAULT_ID, manager.activeId());
		assertTrue(manager.active().builtIn);
		assertFalse(manager.isEdited());
		assertTrue(manager.userThemes().isEmpty());
	}

	private static ThemeManager manager(TestConfig config)
	{
		return new ThemeManager(config::write, config, new Gson());
	}

	private static final class TestConfig implements BetterRightClickConfig
	{
		private String userThemes = "[]";
		private String activeTheme = BuiltInThemes.DEFAULT_ID;
		private int writes;

		@Override
		public String userThemes()
		{
			return userThemes;
		}

		@Override
		public String activeTheme()
		{
			return activeTheme;
		}

		private void write(String key, String value)
		{
			writes++;
			if (BetterRightClickConfig.KEY_USER_THEMES.equals(key))
			{
				userThemes = value;
			}
			else if (BetterRightClickConfig.KEY_ACTIVE_THEME.equals(key))
			{
				activeTheme = value;
			}
		}
	}
}
