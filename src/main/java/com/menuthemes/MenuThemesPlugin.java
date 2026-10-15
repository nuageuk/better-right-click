package com.menuthemes;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.api.events.BeforeMenuRender;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Menu Themes"
)
public class MenuThemesPlugin extends Plugin
{
	@Inject
	private OverlayManager overlayManager;

	@Inject
	private MenuOverlay menuOverlay;

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(menuOverlay);
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

	@Provides
	MenuThemesConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MenuThemesConfig.class);
	}
}
