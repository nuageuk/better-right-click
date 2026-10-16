package com.rightclickstyler;

import com.google.gson.Gson;
import java.awt.Color;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import net.runelite.client.config.ConfigItem;
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

		config.current().values.put(RightClickStylerConfig.KEY_USER_THEMES, "[]");
		config.current().values.put(RightClickStylerConfig.KEY_ACTIVE_THEME, BuiltInThemes.DEFAULT_ID);
		manager.reload();

		assertEquals(writesBeforeReload, config.writes);
		assertEquals(BuiltInThemes.DEFAULT_ID, manager.activeId());
		assertTrue(manager.active().builtIn);
		assertFalse(manager.isEdited());
		assertTrue(manager.userThemes().isEmpty());
	}

	@Test
	public void persistedThemesUseOriginalKeyAndSurviveConfigurationReset()
	{
		TestConfig config = new TestConfig();
		config.current().showSidebarButton = false;
		config.current().highlightsEnabled = false;
		config.current().targetMode = TargetMode.CUSTOM;
		ThemeManager manager = manager(config);
		assertTrue(manager.saveAsNew("Saved theme"));
		String savedId = manager.activeId();

		assertTrue(config.current().values.containsKey(RightClickStylerConfig.KEY_USER_THEMES));
		config.resetAnnotatedConfiguration();
		manager.reload();

		assertEquals(BuiltInThemes.DEFAULT_ID, manager.activeId());
		assertTrue(manager.active().builtIn);
		assertEquals(1, manager.userThemes().size());
		assertEquals(savedId, manager.userThemes().get(0).id);
		assertTrue(config.showSidebarButton());
		assertTrue(config.highlightsEnabled());
		assertEquals(TargetMode.GAME, config.targetMode());
	}

	@Test
	public void userThemesAreNotAnAnnotatedResetItem()
	{
		for (Method method : RightClickStylerConfig.class.getDeclaredMethods())
		{
			ConfigItem item = method.getAnnotation(ConfigItem.class);
			if (item != null)
			{
				assertNotEquals(RightClickStylerConfig.KEY_USER_THEMES, item.keyName());
			}
		}
	}

	@Test
	public void reloadKeepsProfilesIsolated()
	{
		TestConfig config = new TestConfig();
		ThemeManager manager = manager(config);
		assertTrue(manager.saveAsNew("Profile one"));
		String profileOneId = manager.activeId();

		config.switchProfile("two");
		manager.reload();
		assertTrue(manager.userThemes().isEmpty());
		assertEquals(BuiltInThemes.DEFAULT_ID, manager.activeId());
		assertTrue(manager.saveAsNew("Profile two"));
		String profileTwoId = manager.activeId();

		config.switchProfile("one");
		manager.reload();
		assertEquals(profileOneId, manager.activeId());
		assertEquals(profileOneId, manager.userThemes().get(0).id);
		assertNotEquals(profileTwoId, manager.userThemes().get(0).id);
		assertFalse(manager.isEdited());
	}

	private static ThemeManager manager(TestConfig config)
	{
		return new ThemeManager(config::readThemes, config::write, config, new Gson());
	}

	private static final class TestConfig implements RightClickStylerConfig
	{
		private final Map<String, ProfileState> profiles = new HashMap<>();
		private String profile = "one";
		private int writes;

		private TestConfig()
		{
			profiles.put(profile, new ProfileState());
		}

		@Override
		public String activeTheme()
		{
			return current().values.getOrDefault(RightClickStylerConfig.KEY_ACTIVE_THEME,
				BuiltInThemes.DEFAULT_ID);
		}

		@Override
		public boolean showSidebarButton()
		{
			return current().showSidebarButton;
		}

		@Override
		public boolean highlightsEnabled()
		{
			return current().highlightsEnabled;
		}

		@Override
		public TargetMode targetMode()
		{
			return current().targetMode;
		}

		private String readThemes()
		{
			return current().values.getOrDefault(RightClickStylerConfig.KEY_USER_THEMES, "[]");
		}

		private void write(String key, String value)
		{
			writes++;
			current().values.put(key, value);
		}

		private void resetAnnotatedConfiguration()
		{
			current().values.remove(RightClickStylerConfig.KEY_ACTIVE_THEME);
			current().showSidebarButton = true;
			current().highlightsEnabled = true;
			current().targetMode = TargetMode.GAME;
		}

		private void switchProfile(String profile)
		{
			this.profile = profile;
			profiles.computeIfAbsent(profile, key -> new ProfileState());
		}

		private ProfileState current()
		{
			return profiles.get(profile);
		}
	}

	private static final class ProfileState
	{
		private final Map<String, String> values = new HashMap<>();
		private boolean showSidebarButton = true;
		private boolean highlightsEnabled = true;
		private TargetMode targetMode = TargetMode.GAME;
	}
}
