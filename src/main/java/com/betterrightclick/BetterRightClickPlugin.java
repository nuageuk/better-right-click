package com.betterrightclick;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.api.events.BeforeMenuRender;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Better Right Click"
)
public class BetterRightClickPlugin extends Plugin
{
	private static final String NAME = "Better Right Click";
	private static final String INTERFACE_STYLES_GROUP = "interfaceStyles";
	private static final String HD_MENU_KEY = "hdMenu";
	private static final String HD_MENU_WARNING = NAME + ": turn off Interface Styles' HD menu for best results.";
	private static final int NAV_PRIORITY = 5;

	private static final int ICON_SIZE = 16;
	private static final int ICON_ARC = 3;
	private static final int ICON_BAR_X = 3;
	private static final int ICON_BAR_WIDTH = 10;
	private static final int ICON_BAR_HEIGHT = 2;
	private static final int[] ICON_BAR_YS = {4, 7, 10};
	private static final Color ICON_BACKGROUND = new Color(0x2C313A);
	private static final Color ICON_FOREGROUND = new Color(0x4C8DF6);

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

	// Built in startUp(), not injected as a field: RuneLite's look-and-feel (RuneLiteLAF) is only
	// installed by ClientUI.init(), which runs after plugins are loaded but before startUp() is
	// called. A Swing-holding field would be constructed during the load phase - before the LAF is
	// set - and would keep the default (light) Swing styling forever, since Swing never retroactively
	// re-skins an existing component tree when the LAF changes later.
	private BetterRightClickPanel panel;
	private NavigationButton navigationButton;
	private boolean warnedAboutHdMenu;

	@Override
	protected void startUp()
	{
		// Build the panel first: it is the step that can throw, and the overlay (which consumes the
		// vanilla menu) shouldn't be left registered if startUp fails part-way.
		panel = getInjector().getInstance(BetterRightClickPanel.class);
		navigationButton = NavigationButton.builder()
			.tooltip(NAME)
			.icon(createIcon())
			.priority(NAV_PRIORITY)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navigationButton);

		menuOverlay.resetFailure();
		overlayManager.add(menuOverlay);

		warnIfHdMenuEnabled(configManager.getConfiguration(INTERFACE_STYLES_GROUP, HD_MENU_KEY));
		captureLocalPlayer();
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
		menuOverlay.resetFailure();
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

	/**
	 * The menu box is sized by the client from the entry targets, so a hidden level has to be stripped
	 * from the entry itself, not just left undrawn, or its width is still reserved.
	 */
	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (config.combatLevelMode() != CombatLevelMode.HIDE)
		{
			return;
		}
		MenuEntry entry = event.getMenuEntry();
		String target = entry.getTarget();
		if (target == null)
		{
			return;
		}
		String stripped = MenuRenderer.stripLevel(target);
		if (stripped != target)
		{
			entry.setTarget(stripped);
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			captureLocalPlayer();
		}
		else if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			localPlayer.clear();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (INTERFACE_STYLES_GROUP.equals(event.getGroup()) && HD_MENU_KEY.equals(event.getKey()))
		{
			warnIfHdMenuEnabled(event.getNewValue());
		}
	}

	@Provides
	BetterRightClickConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BetterRightClickConfig.class);
	}

	/**
	 * Retries each client tick until the local player exists (it is often still null on the
	 * LOGGED_IN event), and gives up quietly if we are no longer logged in.
	 */
	private void captureLocalPlayer()
	{
		clientThread.invokeLater(() ->
		{
			if (client.getGameState() != GameState.LOGGED_IN)
			{
				return true;
			}
			Player player = client.getLocalPlayer();
			if (player == null)
			{
				return false;
			}
			localPlayer.set(player.getCombatLevel());
			return true;
		});
	}

	// Called from startUp and ConfigChanged, neither of which is guaranteed to be on the client thread.
	private void warnIfHdMenuEnabled(String hdMenuValue)
	{
		if (!warnedAboutHdMenu && Boolean.parseBoolean(hdMenuValue))
		{
			warnedAboutHdMenu = true;
			clientThread.invokeLater(() ->
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", HD_MENU_WARNING, null));
		}
	}

	private static BufferedImage createIcon()
	{
		BufferedImage icon = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = icon.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(ICON_BACKGROUND);
		g.fillRoundRect(0, 0, ICON_SIZE, ICON_SIZE, ICON_ARC, ICON_ARC);
		g.setColor(ICON_FOREGROUND);
		for (int y : ICON_BAR_YS)
		{
			g.fillRect(ICON_BAR_X, y, ICON_BAR_WIDTH, ICON_BAR_HEIGHT);
		}
		g.dispose();
		return icon;
	}
}
