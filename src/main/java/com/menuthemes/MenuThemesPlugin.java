package com.menuthemes;

import com.google.inject.Provides;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.BeforeMenuRender;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Menu Themes"
)
public class MenuThemesPlugin extends Plugin
{
	private static final String INTERFACE_STYLES_GROUP = "interfaceStyles";
	private static final String HD_MENU_KEY = "hdMenu";

	// Keys written by applyPreset(); editing any of these manually reverts the preset to Custom.
	private static final Set<String> PRESET_KEYS = new HashSet<>(Arrays.asList(
		MenuThemesConfig.KEY_BG, MenuThemesConfig.KEY_BORDER, MenuThemesConfig.KEY_HEADER,
		MenuThemesConfig.KEY_HEADER_TEXT, MenuThemesConfig.KEY_TEXT, MenuThemesConfig.KEY_HOVER,
		MenuThemesConfig.KEY_ACCENT, MenuThemesConfig.KEY_RADIUS, MenuThemesConfig.KEY_OPACITY,
		MenuThemesConfig.KEY_HOVER_STYLE));

	@Inject
	private Client client;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private MenuOverlay menuOverlay;

	@Inject
	private MenuThemesConfig config;

	@Inject
	private ConfigManager configManager;

	private boolean warnedAboutHdMenu;
	private boolean applyingPreset;

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(menuOverlay);
		warnIfHdMenuEnabled(configManager.getConfiguration(INTERFACE_STYLES_GROUP, HD_MENU_KEY));
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(menuOverlay);
	}

	@Subscribe
	public void onBeforeMenuRender(BeforeMenuRender event)
	{
		event.consume();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (MenuThemesConfig.GROUP.equals(event.getGroup()))
		{
			if (MenuThemesConfig.KEY_THEME.equals(event.getKey()))
			{
				applyPreset(config.theme());
			}
			else if (!applyingPreset && PRESET_KEYS.contains(event.getKey()) && config.theme() != Preset.CUSTOM)
			{
				configManager.setConfiguration(MenuThemesConfig.GROUP, MenuThemesConfig.KEY_THEME, Preset.CUSTOM);
			}
		}
		else if (INTERFACE_STYLES_GROUP.equals(event.getGroup()) && HD_MENU_KEY.equals(event.getKey()))
		{
			warnIfHdMenuEnabled(event.getNewValue());
		}
	}

	private void warnIfHdMenuEnabled(String hdMenuValue)
	{
		if (!warnedAboutHdMenu && Boolean.parseBoolean(hdMenuValue))
		{
			warnedAboutHdMenu = true;
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Menu Themes: turn off Interface Styles' HD menu for best results.", null);
		}
	}

	private void applyPreset(Preset preset)
	{
		if (preset == Preset.CUSTOM)
		{
			return;
		}

		applyingPreset = true;
		try
		{
			configManager.setConfiguration(MenuThemesConfig.GROUP, MenuThemesConfig.KEY_BG, preset.bg);
			configManager.setConfiguration(MenuThemesConfig.GROUP, MenuThemesConfig.KEY_BORDER, preset.border);
			configManager.setConfiguration(MenuThemesConfig.GROUP, MenuThemesConfig.KEY_HEADER, preset.header);
			configManager.setConfiguration(MenuThemesConfig.GROUP, MenuThemesConfig.KEY_HEADER_TEXT, preset.headerText);
			configManager.setConfiguration(MenuThemesConfig.GROUP, MenuThemesConfig.KEY_TEXT, preset.text);
			configManager.setConfiguration(MenuThemesConfig.GROUP, MenuThemesConfig.KEY_HOVER, preset.hover);
			configManager.setConfiguration(MenuThemesConfig.GROUP, MenuThemesConfig.KEY_ACCENT, preset.accent);
			configManager.setConfiguration(MenuThemesConfig.GROUP, MenuThemesConfig.KEY_RADIUS, preset.radius);
			configManager.setConfiguration(MenuThemesConfig.GROUP, MenuThemesConfig.KEY_OPACITY, preset.opacity);
			configManager.setConfiguration(MenuThemesConfig.GROUP, MenuThemesConfig.KEY_HOVER_STYLE, preset.hoverStyle);
		}
		finally
		{
			applyingPreset = false;
		}
	}

	@Provides
	MenuThemesConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MenuThemesConfig.class);
	}
}
