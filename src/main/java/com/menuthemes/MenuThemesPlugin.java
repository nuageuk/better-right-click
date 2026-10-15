package com.menuthemes;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.BeforeMenuRender;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Menu Themes"
)
public class MenuThemesPlugin extends Plugin
{
	private static final String INTERFACE_STYLES_GROUP = "interfaceStyles";
	private static final String HD_MENU_KEY = "hdMenu";

	@Inject
	private Client client;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private MenuOverlay menuOverlay;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private MenuThemesPanel panel;

	private NavigationButton navigationButton;
	private boolean warnedAboutHdMenu;

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(menuOverlay);

		navigationButton = NavigationButton.builder()
			.tooltip("Menu Themes")
			.icon(createIcon())
			.priority(5)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navigationButton);

		warnIfHdMenuEnabled(configManager.getConfiguration(INTERFACE_STYLES_GROUP, HD_MENU_KEY));
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(menuOverlay);
		clientToolbar.removeNavigation(navigationButton);
	}

	@Subscribe
	public void onBeforeMenuRender(BeforeMenuRender event)
	{
		event.consume();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (INTERFACE_STYLES_GROUP.equals(event.getGroup()) && HD_MENU_KEY.equals(event.getKey()))
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

	@Provides
	MenuThemesConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MenuThemesConfig.class);
	}

	private static BufferedImage createIcon()
	{
		BufferedImage icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = icon.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(new Color(0x2C313A));
		g.fillRoundRect(0, 0, 16, 16, 3, 3);
		g.setColor(new Color(0x4C8DF6));
		g.fillRect(3, 4, 10, 2);
		g.fillRect(3, 7, 10, 2);
		g.fillRect(3, 10, 10, 2);
		g.dispose();
		return icon;
	}
}
