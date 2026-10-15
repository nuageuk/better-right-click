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
	// Only touched on the client thread, apart from the reset in shutDown.
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

		onHdMenuSetting(configManager.getConfiguration(INTERFACE_STYLES_GROUP, HD_MENU_KEY));
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
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (INTERFACE_STYLES_GROUP.equals(event.getGroup()) && HD_MENU_KEY.equals(event.getKey()))
		{
			onHdMenuSetting(event.getNewValue());
		}
	}

	@Provides
	BetterRightClickConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BetterRightClickConfig.class);
	}

	// Called from startUp and ConfigChanged, neither of which is guaranteed to be on the EDT or the
	// client thread.
	private void onHdMenuSetting(String hdMenuValue)
	{
		boolean enabled = Boolean.parseBoolean(hdMenuValue);
		BetterRightClickPanel current = panel;
		if (current != null)
		{
			SwingUtilities.invokeLater(() -> current.setHdMenuWarningVisible(enabled));
		}
		if (!enabled)
		{
			return;
		}
		clientThread.invokeLater(() ->
		{
			if (!warnedAboutHdMenu)
			{
				warnedAboutHdMenu = true;
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", HD_MENU_WARNING, null);
			}
		});
	}
}
