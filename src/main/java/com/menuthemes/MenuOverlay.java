package com.menuthemes;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Menu;
import net.runelite.api.MenuEntry;
import net.runelite.api.Point;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.Text;

public class MenuOverlay extends Overlay
{
	private static final Pattern COLOUR_TAG = Pattern.compile("<col=([0-9a-fA-F]{6})>");

	private final Client client;
	private final MenuThemesConfig config;

	@Inject
	private MenuOverlay(Client client, MenuThemesConfig config)
	{
		this.client = client;
		this.config = config;
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
		setPosition(OverlayPosition.DYNAMIC);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!client.isMenuOpen())
		{
			return null;
		}

		Menu menu = client.getMenu();
		int x = menu.getMenuX();
		int y = menu.getMenuY();
		int width = menu.getMenuWidth();
		int height = menu.getMenuHeight();
		int headerHeight = config.headerHeight();
		int rowHeight = config.rowHeight();
		int textOffsetX = config.textOffsetX();
		int textBaselineOffset = config.textBaselineOffset();

		graphics.setFont(FontManager.getRunescapeFont());
		FontMetrics metrics = graphics.getFontMetrics();

		graphics.setColor(config.bgColour());
		graphics.fillRect(x, y, width, height);

		graphics.setColor(config.headerColour());
		graphics.fillRect(x, y, width, headerHeight);
		graphics.setColor(config.headerTextColour());
		graphics.drawString("Choose Option", x + textOffsetX, y + textBaselineOffset);

		MenuEntry[] entries = menu.getMenuEntries();
		Point mouse = client.getMouseCanvasPosition();

		for (int row = 0; row < entries.length; row++)
		{
			MenuEntry entry = entries[entries.length - 1 - row];
			int rowY = y + headerHeight + row * rowHeight;
			Rectangle rowBounds = new Rectangle(x, rowY, width, rowHeight);

			if (mouse != null && rowBounds.contains(mouse.getX(), mouse.getY()))
			{
				graphics.setColor(config.hoverColour());
				graphics.fillRect(rowBounds.x, rowBounds.y, rowBounds.width, rowBounds.height);
			}

			if (config.showHitRows())
			{
				graphics.setColor(Color.MAGENTA);
				graphics.drawRect(rowBounds.x, rowBounds.y, rowBounds.width - 1, rowBounds.height - 1);
			}

			int textX = x + textOffsetX;
			int textY = rowY + textBaselineOffset;
			String option = Text.removeTags(entry.getOption());

			graphics.setColor(config.textColour());
			graphics.drawString(option, textX, textY);

			String target = entry.getTarget();
			if (target != null && !target.isEmpty())
			{
				int targetX = textX + metrics.stringWidth(option + " ");
				graphics.setColor(targetColour(target));
				graphics.drawString(Text.removeTags(target), targetX, textY);
			}
		}

		graphics.setColor(config.borderColour());
		graphics.drawRect(x, y, width - 1, height - 1);

		return null;
	}

	private static Color targetColour(String target)
	{
		Matcher matcher = COLOUR_TAG.matcher(target);
		if (matcher.find())
		{
			return Color.decode("#" + matcher.group(1));
		}
		return Color.YELLOW;
	}
}
