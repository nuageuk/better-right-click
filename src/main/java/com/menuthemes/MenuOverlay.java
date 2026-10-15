package com.menuthemes;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.IndexedSprite;
import net.runelite.api.Menu;
import net.runelite.api.MenuEntry;
import net.runelite.api.Point;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;

public class MenuOverlay extends Overlay
{
	private static final Pattern TAG = Pattern.compile("<([^>]*)>");
	private static final Pattern COL = Pattern.compile("col=([0-9a-fA-F]{1,6})", Pattern.CASE_INSENSITIVE);
	private static final Pattern IMG = Pattern.compile("img=(\\d+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern LEVEL = Pattern.compile("\\(level-(-?\\d+)\\)", Pattern.CASE_INSENSITIVE);

	private final Client client;
	private final MenuThemesConfig config;

	private FontChoice cachedFontChoice;
	private int cachedFontSize;
	private Font cachedFont;
	private Font cachedBoldFont;

	private String cachedRulesRaw;
	private Map<String, Color> cachedRules = Collections.emptyMap();

	private final Map<Integer, BufferedImage> modIconCache = new HashMap<>();

	private Theme cachedTheme;

	@Inject
	private MenuOverlay(Client client, MenuThemesConfig config)
	{
		this.client = client;
		this.config = config;
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
		setPosition(OverlayPosition.DYNAMIC);
	}

	void invalidateTheme()
	{
		cachedTheme = null;
	}

	private Theme resolveTheme()
	{
		if (cachedTheme == null)
		{
			cachedTheme = Theme.of(config);
		}
		return cachedTheme;
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

		Theme theme = resolveTheme();

		boolean headerOn = config.headerEnabled();
		int headerHeight = headerOn ? config.headerHeight() : 0;
		int rowHeight = config.rowHeight();
		int textOffsetX = config.textOffsetX();
		int baselineNudge = config.baselineNudge();
		int radius = theme.radius;
		int alpha = (int) Math.round(theme.opacity * 255 / 100.0);
		boolean shadow = config.textShadow();
		Color accent = theme.accent;
		HoverStyle hoverStyle = theme.hoverStyle;

		Font font = resolveFont();
		graphics.setFont(font);
		FontMetrics metrics = graphics.getFontMetrics();
		int headerTextY = y + centreOffset(headerHeight, metrics, baselineNudge);
		int rowTextOffsetY = centreOffset(rowHeight, metrics, baselineNudge);

		Shape menuShape = radius > 0
			? new RoundRectangle2D.Float(x, y, width, height, radius, radius)
			: new Rectangle(x, y, width, height);

		if (config.dropShadowEnabled())
		{
			for (int i = 3; i >= 1; i--)
			{
				Shape shadowShape = radius > 0
					? new RoundRectangle2D.Float(x + i, y + i, width, height, radius, radius)
					: new Rectangle(x + i, y + i, width, height);
				graphics.setColor(new Color(0, 0, 0, 15 * (4 - i)));
				graphics.fill(shadowShape);
			}
		}

		graphics.setColor(ColorUtil.colorWithAlpha(theme.bg, alpha));
		graphics.fill(menuShape);

		Shape previousClip = graphics.getClip();
		graphics.setClip(menuShape);

		if (headerOn)
		{
			graphics.setColor(ColorUtil.colorWithAlpha(theme.header, alpha));
			graphics.fillRect(x, y, width, headerHeight);
			graphics.setFont(font);
			drawRun(graphics, "Choose Option", x + textOffsetX, headerTextY, theme.headerText, shadow);
		}

		MenuEntry[] entries = menu.getMenuEntries();
		Point mouse = client.getMouseCanvasPosition();
		boolean highlightsOn = config.highlightsEnabled();
		Map<String, Color> rules = highlightsOn ? highlightRules() : Collections.emptyMap();

		for (int row = 0; row < entries.length; row++)
		{
			MenuEntry entry = entries[entries.length - 1 - row];
			int rowY = y + headerHeight + row * rowHeight;
			boolean hovered = mouse != null && mouse.getX() >= x && mouse.getX() < x + width
				&& mouse.getY() >= rowY && mouse.getY() < rowY + rowHeight;

			if (hovered && hoverStyle != HoverStyle.NONE)
			{
				applyHover(graphics, hoverStyle, x, rowY, width, rowHeight, theme.hover, accent, alpha);
			}

			if (config.showHitRows())
			{
				graphics.setColor(Color.MAGENTA);
				graphics.drawRect(x, rowY, width - 1, rowHeight - 1);
			}

			int textY = rowY + rowTextOffsetY;
			int cursorX = x + textOffsetX;

			String rawOption = entry.getOption();
			String plainOption = Text.removeTags(rawOption).toLowerCase(Locale.ROOT).trim();
			Color highlight = rules.get(plainOption);

			graphics.setFont(highlight != null && config.highlightsBold() ? cachedBoldFont : font);

			if (hovered && hoverStyle == HoverStyle.TEXT)
			{
				cursorX += drawRun(graphics, Text.removeTags(rawOption), cursorX, textY, accent, shadow);
			}
			else if (highlight != null)
			{
				cursorX += drawRun(graphics, Text.removeTags(rawOption), cursorX, textY, highlight, shadow);
			}
			else
			{
				for (Run run : parseRuns(rawOption))
				{
					if (run.iconIndex >= 0)
					{
						cursorX += drawIcon(graphics, run.iconIndex, cursorX, textY);
						continue;
					}
					cursorX += drawRun(graphics, run.text, cursorX, textY,
						run.tagColour != null ? run.tagColour : theme.text, shadow);
				}
			}

			graphics.setFont(font);

			String target = entry.getTarget();
			if (target != null && !target.isEmpty())
			{
				cursorX += graphics.getFontMetrics().stringWidth(" ");
				TargetKind kind = classify(entry);
				for (Run run : parseRuns(target))
				{
					for (Run part : splitLevel(run))
					{
						if (part.iconIndex >= 0)
						{
							cursorX += drawIcon(graphics, part.iconIndex, cursorX, textY);
							continue;
						}
						Color colour = resolveTargetColour(part, kind);
						if (colour == null)
						{
							continue;
						}
						cursorX += drawRun(graphics, part.text, cursorX, textY, colour, shadow);
					}
				}
			}
		}

		graphics.setClip(previousClip);

		if (config.borderEnabled())
		{
			graphics.setColor(theme.border);
			if (radius > 0)
			{
				graphics.draw(new RoundRectangle2D.Float(x, y, width - 1, height - 1, radius, radius));
			}
			else
			{
				graphics.drawRect(x, y, width - 1, height - 1);
			}
		}

		return null;
	}

	private static int centreOffset(int boxHeight, FontMetrics metrics, int nudge)
	{
		return (boxHeight - (metrics.getAscent() + metrics.getDescent())) / 2 + metrics.getAscent() + nudge;
	}

	private static void applyHover(Graphics2D graphics, HoverStyle style, int x, int rowY, int width, int rowHeight,
		Color hoverColour, Color accent, int alpha)
	{
		switch (style)
		{
			case FILL:
				graphics.setColor(ColorUtil.colorWithAlpha(hoverColour, alpha));
				graphics.fillRect(x, rowY, width, rowHeight);
				break;
			case BAR:
				graphics.setColor(ColorUtil.colorWithAlpha(hoverColour, alpha));
				graphics.fillRect(x, rowY, width, rowHeight);
				graphics.setColor(accent);
				graphics.fillRect(x, rowY, 3, rowHeight);
				break;
			case OUTLINE:
				graphics.setColor(accent);
				graphics.drawRect(x + 1, rowY + 1, width - 3, rowHeight - 3);
				break;
			case TEXT:
			case NONE:
				break;
		}
	}

	private static int drawRun(Graphics2D graphics, String text, int x, int y, Color colour, boolean shadow)
	{
		if (text.isEmpty())
		{
			return 0;
		}
		if (shadow)
		{
			graphics.setColor(Color.BLACK);
			graphics.drawString(text, x + 1, y + 1);
		}
		graphics.setColor(colour);
		graphics.drawString(text, x, y);
		return graphics.getFontMetrics().stringWidth(text);
	}

	private int drawIcon(Graphics2D graphics, int index, int x, int y)
	{
		BufferedImage icon = modIcon(index);
		if (icon == null)
		{
			return 0;
		}
		int height = graphics.getFontMetrics().getAscent();
		int width = Math.max(1, icon.getWidth() * height / icon.getHeight());
		graphics.drawImage(icon, x, y - height, width, height, null);
		return width + 1;
	}

	private BufferedImage modIcon(int index)
	{
		IndexedSprite[] icons = client.getModIcons();
		if (index < 0 || icons == null || index >= icons.length || icons[index] == null)
		{
			return null;
		}
		return modIconCache.computeIfAbsent(index, i -> buildModIcon(icons[i]));
	}

	private static BufferedImage buildModIcon(IndexedSprite sprite)
	{
		int width = sprite.getWidth();
		int height = sprite.getHeight();
		byte[] pixels = sprite.getPixels();
		int[] palette = sprite.getPalette();
		int[] argb = new int[width * height];
		for (int i = 0; i < pixels.length && i < argb.length; i++)
		{
			int paletteIndex = pixels[i] & 0xFF;
			argb[i] = paletteIndex == 0 ? 0 : (0xFF000000 | palette[paletteIndex]);
		}
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		image.setRGB(0, 0, width, height, argb, 0, width);
		return image;
	}

	private Font resolveFont()
	{
		FontChoice choice = config.font();
		int size = config.fontSize();
		if (choice != cachedFontChoice || size != cachedFontSize)
		{
			cachedFontChoice = choice;
			cachedFontSize = size;
			cachedFont = buildFont(choice, size);
			cachedBoldFont = buildBoldFont(choice, size);
		}
		return cachedFont;
	}

	private static Font buildFont(FontChoice choice, int size)
	{
		switch (choice)
		{
			case RS_SMALL:
				return FontManager.getRunescapeSmallFont();
			case RS_BOLD:
				return FontManager.getRunescapeBoldFont();
			case SANS:
				return new Font(Font.SANS_SERIF, Font.PLAIN, size);
			case MONO:
				return new Font(Font.MONOSPACED, Font.PLAIN, size);
			case RS_REGULAR:
			default:
				return FontManager.getRunescapeFont();
		}
	}

	private static Font buildBoldFont(FontChoice choice, int size)
	{
		switch (choice)
		{
			case SANS:
				return new Font(Font.SANS_SERIF, Font.BOLD, size);
			case MONO:
				return new Font(Font.MONOSPACED, Font.BOLD, size);
			default:
				return FontManager.getRunescapeBoldFont();
		}
	}

	private Map<String, Color> highlightRules()
	{
		String raw = config.highlightRules();
		if (!raw.equals(cachedRulesRaw))
		{
			cachedRulesRaw = raw;
			cachedRules = parseHighlightRules(raw);
		}
		return cachedRules;
	}

	private static Map<String, Color> parseHighlightRules(String raw)
	{
		Map<String, Color> rules = new HashMap<>();
		for (String line : raw.split("\n"))
		{
			int eq = line.lastIndexOf('=');
			if (eq <= 0 || eq == line.length() - 1)
			{
				continue;
			}
			String option = line.substring(0, eq).trim().toLowerCase(Locale.ROOT);
			String hex = line.substring(eq + 1).trim();
			if (hex.startsWith("#"))
			{
				hex = hex.substring(1);
			}
			if (option.isEmpty() || !hex.matches("[0-9a-fA-F]{6}"))
			{
				continue;
			}
			rules.put(option, Color.decode("#" + hex));
		}
		return rules;
	}

	private Color resolveTargetColour(Run run, TargetKind kind)
	{
		if (run.isLevel)
		{
			CombatLevelMode mode = config.combatLevelMode();
			if (mode == CombatLevelMode.HIDE)
			{
				return null;
			}
			if (mode == CombatLevelMode.CUSTOM)
			{
				int local = client.getLocalPlayer() != null ? client.getLocalPlayer().getCombatLevel() : 0;
				if (run.level < local)
				{
					return config.lowerLevelColour();
				}
				if (run.level > local)
				{
					return config.higherLevelColour();
				}
				return config.sameLevelColour();
			}
			return run.tagColour != null ? run.tagColour : Color.YELLOW;
		}

		if (config.targetMode() == TargetMode.CUSTOM)
		{
			switch (kind)
			{
				case NPC:
					return config.npcColour();
				case PLAYER:
					return config.playerColour();
				case ITEM:
					return config.itemColour();
				case OBJECT:
					return config.objectColour();
				default:
					return config.otherColour();
			}
		}

		return run.tagColour != null ? run.tagColour : Color.YELLOW;
	}

	private static TargetKind classify(MenuEntry entry)
	{
		if (entry.getNpc() != null)
		{
			return TargetKind.NPC;
		}
		if (entry.getPlayer() != null)
		{
			return TargetKind.PLAYER;
		}
		if (entry.getItemId() != -1)
		{
			return TargetKind.ITEM;
		}
		switch (entry.getType())
		{
			case GAME_OBJECT_FIRST_OPTION:
			case GAME_OBJECT_SECOND_OPTION:
			case GAME_OBJECT_THIRD_OPTION:
			case GAME_OBJECT_FOURTH_OPTION:
			case GAME_OBJECT_FIFTH_OPTION:
				return TargetKind.OBJECT;
			default:
				return TargetKind.OTHER;
		}
	}

	private static List<Run> parseRuns(String raw)
	{
		List<Run> runs = new ArrayList<>();
		if (raw == null || raw.isEmpty())
		{
			return runs;
		}
		Matcher matcher = TAG.matcher(raw);
		int last = 0;
		Color current = null;
		while (matcher.find())
		{
			if (matcher.start() > last)
			{
				runs.add(new Run(raw.substring(last, matcher.start()), current));
			}
			last = matcher.end();

			String inner = matcher.group(1);
			Matcher col = COL.matcher(inner);
			Matcher img = IMG.matcher(inner);
			if (col.matches())
			{
				current = new Color(Integer.parseInt(col.group(1), 16));
			}
			else if (inner.equalsIgnoreCase("/col"))
			{
				current = null;
			}
			else if (img.matches())
			{
				runs.add(Run.icon(Integer.parseInt(img.group(1))));
			}
			// else: strip silently, e.g. <u>, </u>, <shad>, <br>
		}
		if (last < raw.length())
		{
			runs.add(new Run(raw.substring(last), current));
		}
		return runs;
	}

	private static List<Run> splitLevel(Run run)
	{
		Matcher matcher = LEVEL.matcher(run.text);
		if (!matcher.find())
		{
			return Collections.singletonList(run);
		}

		List<Run> parts = new ArrayList<>();
		int last = 0;
		matcher.reset();
		while (matcher.find())
		{
			if (matcher.start() > last)
			{
				parts.add(new Run(run.text.substring(last, matcher.start()), run.tagColour));
			}
			parts.add(new Run(run.text.substring(matcher.start(), matcher.end()), run.tagColour, true,
				Integer.parseInt(matcher.group(1))));
			last = matcher.end();
		}
		if (last < run.text.length())
		{
			parts.add(new Run(run.text.substring(last), run.tagColour));
		}
		return parts;
	}

	private enum TargetKind
	{
		NPC,
		PLAYER,
		ITEM,
		OBJECT,
		OTHER
	}

	private static final class Run
	{
		private final String text;
		private final Color tagColour;
		private final boolean isLevel;
		private final int level;
		private final int iconIndex;

		private Run(String text, Color tagColour)
		{
			this(text, tagColour, false, 0, -1);
		}

		private Run(String text, Color tagColour, boolean isLevel, int level)
		{
			this(text, tagColour, isLevel, level, -1);
		}

		private Run(String text, Color tagColour, boolean isLevel, int level, int iconIndex)
		{
			this.text = text;
			this.tagColour = tagColour;
			this.isLevel = isLevel;
			this.level = level;
			this.iconIndex = iconIndex;
		}

		private static Run icon(int index)
		{
			return new Run("", null, false, 0, index);
		}
	}
}
