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
import net.runelite.client.events.ProfileChanged;
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
	private static final String MENU_ALPHA_KEY = "menuAlpha";
	// PluginManager stores each plugin's enabled state under its lowercased class name in the
	// "runelite" group. Interface Styles is off by default.
	private static final String INTERFACE_STYLES_PLUGIN_KEY = "interfacestylesplugin";
	private static final String INTERFACE_STYLES_WARNING = NAME
		+ ": turn off Interface Styles' High detail menu and set its Menu alpha to 255.";
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
	private BetterRightClickConfig config;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private LocalPlayerSnapshot localPlayer;

	@Inject
	private ThemeManager themeManager;

	// Created in startUp() rather than injected: RuneLite installs its look and feel after plugins
	// load, and Swing components built before that keep the default light styling. Volatile because
	// ConfigChanged can arrive on any thread.
	private volatile BetterRightClickPanel panel;
	// Volatile because ConfigChanged can arrive on any thread.
	private volatile NavigationButton navigationButton;
	// Once per session, and only once it could actually be seen. Client thread only, apart from the
	// reset in shutDown.
	private volatile boolean warnedAboutInterfaceStylesConflict;
	private volatile boolean running;

	@Override
	protected void startUp()
	{
		// Reload before building the panel so enabling the plugin always uses the active profile.
		// Register the overlay only after the panel has been built successfully.
		themeManager.reload();
		panel = getInjector().getInstance(BetterRightClickPanel.class);
		navigationButton = NavigationButton.builder()
			.tooltip(NAME)
			.icon(ImageUtil.loadImageResource(BetterRightClickPlugin.class, "panel_icon.png"))
			.priority(NAV_PRIORITY)
			.panel(panel)
			.build();
		updateSidebarButton();

		menuOverlay.resetFailure();
		overlayManager.add(menuOverlay);

		updateInterfaceStylesWarning();
		running = true;
	}

	@Override
	protected void shutDown()
	{
		running = false;
		overlayManager.remove(menuOverlay);
		if (navigationButton != null)
		{
			clientToolbar.removeNavigation(navigationButton);
			navigationButton = null;
		}
		panel = null;
		warnedAboutInterfaceStylesConflict = false;
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
		if (BetterRightClickConfig.GROUP.equals(event.getGroup())
			&& BetterRightClickConfig.KEY_SHOW_SIDEBAR_BUTTON.equals(event.getKey()))
		{
			updateSidebarButton();
			return;
		}
		if ((INTERFACE_STYLES_GROUP.equals(event.getGroup())
			&& (HD_MENU_KEY.equals(event.getKey()) || MENU_ALPHA_KEY.equals(event.getKey())))
			|| (RuneLiteConfig.GROUP_NAME.equals(event.getGroup()) && INTERFACE_STYLES_PLUGIN_KEY.equals(event.getKey())))
		{
			updateInterfaceStylesWarning();
		}
	}

	@Subscribe
	public void onProfileChanged(ProfileChanged event)
	{
		if (!running)
		{
			return;
		}
		BetterRightClickPanel expectedPanel = panel;
		SwingUtilities.invokeLater(() ->
		{
			if (!running || panel != expectedPanel)
			{
				return;
			}
			themeManager.reload();
			BetterRightClickPanel current = panel;
			if (current != null)
			{
				current.reloadFromConfig();
			}
			updateSidebarButton();
			updateInterfaceStylesWarning();
		});
	}

	@Override
	public void resetConfiguration()
	{
		BetterRightClickPanel expectedPanel = panel;
		Runnable reload = () ->
		{
			if (expectedPanel == null)
			{
				if (!running)
				{
					themeManager.reload();
				}
				return;
			}
			if (!running || panel != expectedPanel)
			{
				return;
			}
			themeManager.reload();
			expectedPanel.reloadFromConfig();
			updateSidebarButton();
			updateInterfaceStylesWarning();
		};

		if (SwingUtilities.isEventDispatchThread())
		{
			reload.run();
		}
		else
		{
			SwingUtilities.invokeLater(reload);
		}
	}

	@Provides
	BetterRightClickConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BetterRightClickConfig.class);
	}

	// ClientToolbar hands both calls to the EDT, and adding or removing twice is harmless.
	private void updateSidebarButton()
	{
		NavigationButton button = navigationButton;
		if (button == null)
		{
			return;
		}
		if (config.showSidebarButton())
		{
			clientToolbar.addNavigation(button);
		}
		else
		{
			clientToolbar.removeNavigation(button);
		}
	}

	/**
	 * Interface Styles conflicts while enabled with its high-detail menu on or its menu alpha not 255.
	 */
	private boolean isInterfaceStylesConflictActive()
	{
		if (!Boolean.parseBoolean(configManager.getConfiguration(RuneLiteConfig.GROUP_NAME, INTERFACE_STYLES_PLUGIN_KEY)))
		{
			return false;
		}
		if (Boolean.parseBoolean(configManager.getConfiguration(INTERFACE_STYLES_GROUP, HD_MENU_KEY)))
		{
			return true;
		}
		Integer menuAlpha = configManager.getConfiguration(INTERFACE_STYLES_GROUP, MENU_ALPHA_KEY, Integer.class);
		return menuAlpha != null && menuAlpha != 255;
	}

	// Called from startUp and ConfigChanged, neither of which is guaranteed to be on the EDT or the
	// client thread.
	private void updateInterfaceStylesWarning()
	{
		boolean active = isInterfaceStylesConflictActive();
		BetterRightClickPanel current = panel;
		if (current != null)
		{
			SwingUtilities.invokeLater(() -> current.setInterfaceStylesWarningVisible(active));
		}
		if (active)
		{
			clientThread.invokeLater(this::warnInChatOnce);
		}
	}

	// Client thread only. Skipped until logged in, so the one warning isn't spent where nobody sees it.
	private void warnInChatOnce()
	{
		if (warnedAboutInterfaceStylesConflict || client.getGameState() != GameState.LOGGED_IN
			|| !isInterfaceStylesConflictActive())
		{
			return;
		}
		warnedAboutInterfaceStylesConflict = true;
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", INTERFACE_STYLES_WARNING, null);
	}
}
