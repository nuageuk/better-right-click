package com.betterrightclick;

import com.google.inject.Provides;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.events.BeforeMenuRender;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

@PluginDescriptor(
	name = BetterRightClickPlugin.NAME
)
public class BetterRightClickPlugin extends Plugin
{
	static final String NAME = "Better Right Click";
	private static final String INTERFACE_STYLES_GROUP = "interfaceStyles";
	private static final String HD_MENU_KEY = "hdMenu";
	// PluginManager stores each plugin's enabled state under its lowercased class name in the
	// "runelite" group. Interface Styles is off by default, as is its hdMenu option.
	private static final String INTERFACE_STYLES_PLUGIN_KEY = "interfacestylesplugin";
	private static final String HD_MENU_WARNING = NAME + ": turn off Interface Styles' HD menu for best results.";
	private static final int NAV_PRIORITY = 5;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private MenuOverlay menuOverlay;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private LocalPlayerSnapshot localPlayer;

	// Created in startUp() rather than injected: RuneLite installs its look and feel after plugins
	// load, and Swing components built before that keep the default light styling. Volatile because
	// ConfigChanged can arrive on any thread.
	private volatile BetterRightClickPanel panel;
	private NavigationButton navigationButton;
	// Once per session, and only once it could actually be seen. Client thread only, apart from the
	// reset in shutDown.
	private volatile boolean warnedAboutHdMenu;

	@Override
	protected void startUp()
	{
		// Build the panel first: it is the step that can throw, and the overlay (which consumes the
		// vanilla menu) shouldn't be left registered if startUp fails part-way.
		panel = getInjector().getInstance(BetterRightClickPanel.class);
		navigationButton = NavigationButton.builder()
			.tooltip(NAME)
			.icon(ImageUtil.loadImageResource(BetterRightClickPlugin.class, "panel_icon.png"))
			.priority(NAV_PRIORITY)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navigationButton);

		menuOverlay.resetFailure();
		overlayManager.add(menuOverlay);

		updateHdMenuWarning();
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(menuOverlay);
		if (navigationButton != null)
		{
			clientToolbar.removeNavigation(navigationButton);
			navigationButton = null;
		}
		panel = null;
		warnedAboutHdMenu = false;
		localPlayer.setOnChange(null);
		localPlayer.clear();
	}

	@Subscribe
	public void onBeforeMenuRender(BeforeMenuRender event)
	{
		if (menuOverlay.shouldDraw())
		{
			event.consume();
		}
	}

	// Polled each tick so level-ups reach the preview; this also covers login, where the local player
	// is often still null on the LOGGED_IN event.
	@Subscribe
	public void onGameTick(GameTick event)
	{
		Player player = client.getLocalPlayer();
		if (player != null)
		{
			localPlayer.set(player.getCombatLevel());
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			localPlayer.clear();
		}
		else if (event.getGameState() == GameState.LOGGED_IN)
		{
			warnInChatOnce();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if ((INTERFACE_STYLES_GROUP.equals(event.getGroup()) && HD_MENU_KEY.equals(event.getKey()))
			|| (RuneLiteConfig.GROUP_NAME.equals(event.getGroup()) && INTERFACE_STYLES_PLUGIN_KEY.equals(event.getKey())))
		{
			updateHdMenuWarning();
		}
	}

	@Provides
	BetterRightClickConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BetterRightClickConfig.class);
	}

	/**
	 * The HD menu only conflicts while the Interface Styles plugin is enabled and its option is on.
	 */
	private boolean isHdMenuActive()
	{
		return Boolean.parseBoolean(configManager.getConfiguration(RuneLiteConfig.GROUP_NAME, INTERFACE_STYLES_PLUGIN_KEY))
			&& Boolean.parseBoolean(configManager.getConfiguration(INTERFACE_STYLES_GROUP, HD_MENU_KEY));
	}

	// Called from startUp and ConfigChanged, neither of which is guaranteed to be on the EDT or the
	// client thread.
	private void updateHdMenuWarning()
	{
		boolean active = isHdMenuActive();
		BetterRightClickPanel current = panel;
		if (current != null)
		{
			SwingUtilities.invokeLater(() -> current.setHdMenuWarningVisible(active));
		}
		if (active)
		{
			clientThread.invokeLater(this::warnInChatOnce);
		}
	}

	// Client thread only. Skipped until logged in, so the one warning isn't spent where nobody sees it.
	private void warnInChatOnce()
	{
		if (warnedAboutHdMenu || client.getGameState() != GameState.LOGGED_IN || !isHdMenuActive())
		{
			return;
		}
		warnedAboutHdMenu = true;
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", HD_MENU_WARNING, null);
	}
}
