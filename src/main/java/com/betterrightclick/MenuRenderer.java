package com.betterrightclick;

import java.awt.Color;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.Client;
import net.runelite.api.IndexedSprite;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;

/**
 * Draws a themed menu. Shared by the live overlay and the sidebar preview, which each resolve their
 * own bounds/hover row and pass in plain {@link Row}s instead of real menu entries.
 * <p>
 * Each caller owns its own instance and only ever uses it from one thread (the overlay from the
 * client thread, the preview from the EDT), so the caches here need no synchronisation. The preview
 * passes a null {@code Client}, so nothing here may touch the client off the client thread.
 */
final class MenuRenderer
{
	// Excludes '<' from the tag body so a stray '<' in text doesn't swallow the following real tag.
	private static final Pattern TAG = Pattern.compile("<([^<>]*)>");
	private static final Pattern COL = Pattern.compile("col=([0-9a-fA-F]{1,6})", Pattern.CASE_INSENSITIVE);
	private static final Pattern IMG = Pattern.compile("img=(\\d{1,4})", Pattern.CASE_INSENSITIVE);
	private static final Pattern LEVEL = Pattern.compile("\\(level-(-?\\d{1,4})\\)", Pattern.CASE_INSENSITIVE);
	// Whitespace followed only by tags/whitespace up to the end, i.e. visually trailing.
	private static final Pattern TRAILING_SPACE = Pattern.compile("\\s+(?=(?:<[^<>]*>|\\s)*$)");
	private static final String CLOSE_COL = "/col";

	private static final String HEADER_TEXT = "Choose Option";
	private static final Color DEFAULT_TAG_COLOUR = Color.YELLOW;
	private static final Color TEXT_SHADOW_COLOUR = Color.BLACK;
	private static final Color HIT_ROW_COLOUR = Color.MAGENTA;
	private static final int TEXT_SHADOW_OFFSET = 1;
	private static final int ICON_GAP = 1;
	private static final int HOVER_BAR_WIDTH = 3;
	private static final int OUTLINE_INSET = 1;
	private static final int MAX_ENTRY_LENGTH = 512;
	// Large enough that a big menu's entries all fit, so a full menu never thrashes the cache.
	private static final int SEGMENT_CACHE_SIZE = 256;
	private static final int MAX_ALPHA = 255;
	private static final double PERCENT = 100.0;

	// Drop shadow: SHADOW_LAYERS offset copies, fading out with distance.
	private static final int SHADOW_LAYERS = 3;
	private static final Color[] SHADOW_COLOURS = {
		new Color(0, 0, 0, 45), new Color(0, 0, 0, 30), new Color(0, 0, 0, 15)
	};

	private final Map<Integer, BufferedImage> modIconCache = new HashMap<>();
	private final Map<String, OptionSegments> optionCache = boundedCache();
	private final Map<String, List<Run>> targetCache = boundedCache();
	private final RoundRectangle2D.Float menuShape = new RoundRectangle2D.Float();
	private final RoundRectangle2D.Float scratchShape = new RoundRectangle2D.Float();
	private final TargetColours targetColours = new TargetColours();

	private Theme cachedTheme;
	private Font font;
	private Font boldFont;
	private Color bgColour;
	private Color headerColour;
	private Color hoverColour;

	private String cachedRulesRaw;
	private Map<String, Color> cachedRules = Collections.emptyMap();

	/**
	 * @param client null when not on the client thread; {@code <img>} tags are then skipped
	 * @param localCombatLevel used to colour combat levels in Custom mode
	 */
	void render(Graphics2D graphics, Client client, Theme theme, BetterRightClickConfig config,
		List<Row> rows, Rectangle bounds, int hoverIndex, int localCombatLevel)
	{
		// Work on a copy so no colour/font/clip change leaks to other overlays or Swing children.
		Graphics2D g = (Graphics2D) graphics.create();
		try
		{
			renderMenu(g, client, theme, config, rows, bounds, hoverIndex, localCombatLevel);
		}
		finally
		{
			g.dispose();
		}
	}

	private void renderMenu(Graphics2D graphics, Client client, Theme theme, BetterRightClickConfig config,
		List<Row> rows, Rectangle bounds, int hoverIndex, int localCombatLevel)
	{
		updateThemeCache(theme, graphics);

		int headerHeight = theme.showHeader ? Limits.headerHeight(config) : 0;
		int textOffsetX = Limits.textOffsetX(config);
		int baselineNudge = Limits.baselineNudge(config);
		FontMetrics metrics = graphics.getFontMetrics(font);

		drawShadow(graphics, theme, bounds);
		Shape menuShape = menuShape(theme, bounds);
		graphics.setColor(bgColour);
		graphics.fill(menuShape);

		Shape previousClip = graphics.getClip();
		graphics.clip(menuShape);
		if (theme.showHeader)
		{
			graphics.setColor(headerColour);
			graphics.fillRect(bounds.x, bounds.y, bounds.width, headerHeight);
			graphics.setFont(font);
			drawRun(graphics, metrics, HEADER_TEXT, bounds.x + textOffsetX,
				bounds.y + centreOffset(headerHeight, metrics, baselineNudge), theme.headerText, theme.textShadow);
		}
		drawRows(graphics, client, theme, config, rows, bounds, hoverIndex, localCombatLevel, metrics,
			bounds.y + headerHeight, textOffsetX, baselineNudge);
		graphics.setClip(previousClip);

		drawBorder(graphics, theme, bounds);
	}

	private void drawRows(Graphics2D graphics, Client client, Theme theme, BetterRightClickConfig config,
		List<Row> rows, Rectangle bounds, int hoverIndex, int localCombatLevel, FontMetrics metrics,
		int firstRowY, int textOffsetX, int baselineNudge)
	{
		FontMetrics highlightMetrics = highlightMetrics(graphics, config, metrics);
		Map<String, Color> rules = highlightRules(config);
		boolean showHitRows = config.showHitRows();
		int rowHeight = Limits.rowHeight(config);
		int rowTextOffsetY = centreOffset(rowHeight, metrics, baselineNudge);
		targetColours.load(config, localCombatLevel);

		for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++)
		{
			int rowY = firstRowY + rowIndex * rowHeight;
			boolean hovered = rowIndex == hoverIndex;
			if (hovered)
			{
				drawHover(graphics, theme, bounds.x, rowY, bounds.width, rowHeight);
			}
			if (showHitRows)
			{
				graphics.setColor(HIT_ROW_COLOUR);
				graphics.drawRect(bounds.x, rowY, bounds.width - 1, rowHeight - 1);
			}
			drawRow(graphics, client, theme, rows.get(rowIndex), rules, highlightMetrics, metrics,
				bounds.x + textOffsetX, rowY + rowTextOffsetY, bounds.x + bounds.width, hovered);
		}
	}

	private void drawRow(Graphics2D graphics, Client client, Theme theme, Row row, Map<String, Color> rules,
		FontMetrics highlightMetrics, FontMetrics metrics, int startX, int textY, int maxX, boolean hovered)
	{
		OptionSegments option = optionSegments(row.option);
		Color highlight = rules.get(option.key);
		FontMetrics optionMetrics = highlight != null ? highlightMetrics : metrics;
		graphics.setFont(optionMetrics.getFont());

		int cursorX = startX;
		if (hovered && theme.hoverStyle == HoverStyle.TEXT)
		{
			cursorX += drawRun(graphics, optionMetrics, option.plain, cursorX, textY, theme.accent, theme.textShadow);
		}
		else if (highlight != null)
		{
			cursorX += drawRun(graphics, optionMetrics, option.plain, cursorX, textY, highlight, theme.textShadow);
		}
		else
		{
			List<Run> runs = option.runs;
			for (int i = 0; i < runs.size(); i++)
			{
				Run run = runs.get(i);
				if (cursorX >= maxX)
				{
					return;
				}
				cursorX += run.iconIndex >= 0
					? drawIcon(graphics, optionMetrics, client, run.iconIndex, cursorX, textY)
					: drawRun(graphics, optionMetrics, run.text, cursorX, textY,
						run.tagColour != null ? run.tagColour : theme.text, theme.textShadow);
			}
		}

		graphics.setFont(metrics.getFont());
		if (row.target == null || row.target.isEmpty())
		{
			return;
		}
		cursorX += metrics.charWidth(' ');
		List<Run> runs = targetRuns(row.target, targetColours.levelMode == CombatLevelMode.HIDE);
		for (int i = 0; i < runs.size(); i++)
		{
			Run run = runs.get(i);
			if (cursorX >= maxX)
			{
				return;
			}
			if (run.iconIndex >= 0)
			{
				cursorX += drawIcon(graphics, metrics, client, run.iconIndex, cursorX, textY);
				continue;
			}
			cursorX += drawRun(graphics, metrics, run.text, cursorX, textY, targetColours.resolve(run, row.kind),
				theme.textShadow);
		}
	}

	/**
	 * Width of the widest row (and the header) plus horizontal padding, measured the way the rows are
	 * drawn. Used instead of the game's menu width, which adds its own padding and counts hidden levels.
	 */
	int measureWidth(Graphics2D graphics, Client client, Theme theme, BetterRightClickConfig config, List<Row> rows)
	{
		updateThemeCache(theme, graphics);
		FontMetrics metrics = graphics.getFontMetrics(font);
		FontMetrics highlightMetrics = highlightMetrics(graphics, config, metrics);
		Map<String, Color> rules = highlightRules(config);
		boolean hideLevel = config.combatLevelMode() == CombatLevelMode.HIDE;

		int widest = theme.showHeader ? metrics.stringWidth(HEADER_TEXT) : 0;
		for (Row row : rows)
		{
			OptionSegments option = optionSegments(row.option);
			FontMetrics optionMetrics = rules.containsKey(option.key) ? highlightMetrics : metrics;
			int width = runsWidth(client, optionMetrics, option.runs);
			if (row.target != null && !row.target.isEmpty())
			{
				width += metrics.charWidth(' ') + runsWidth(client, metrics, targetRuns(row.target, hideLevel));
			}
			widest = Math.max(widest, width);
		}
		return widest + Limits.textOffsetX(config) * 2;
	}

	private int runsWidth(Client client, FontMetrics metrics, List<Run> runs)
	{
		int width = 0;
		for (Run run : runs)
		{
			if (run.iconIndex < 0)
			{
				width += metrics.stringWidth(run.text);
				continue;
			}
			BufferedImage icon = modIcon(client, run.iconIndex);
			if (icon != null)
			{
				width += iconWidth(icon, iconHeight(metrics)) + ICON_GAP;
			}
		}
		return width;
	}

	private FontMetrics highlightMetrics(Graphics2D graphics, BetterRightClickConfig config, FontMetrics metrics)
	{
		return config.highlightsEnabled() && config.highlightsBold() ? graphics.getFontMetrics(boldFont) : metrics;
	}

	private Map<String, Color> highlightRules(BetterRightClickConfig config)
	{
		return config.highlightsEnabled() ? rules(config.highlightRules()) : Collections.emptyMap();
	}

	private void updateThemeCache(Theme theme, Graphics2D graphics)
	{
		if (theme == cachedTheme && font != null && boldFont != null)
		{
			return;
		}
		cachedTheme = theme;
		optionCache.clear();
		targetCache.clear();

		Font fallback = graphics.getFont();
		font = orFallback(buildFont(theme.font, theme.fontSize), fallback);
		boldFont = orFallback(buildBoldFont(theme.font, theme.fontSize), font);

		int alpha = (int) Math.round(theme.opacity * MAX_ALPHA / PERCENT);
		bgColour = ColorUtil.colorWithAlpha(theme.bg, alpha);
		headerColour = ColorUtil.colorWithAlpha(theme.header, alpha);
		hoverColour = ColorUtil.colorWithAlpha(theme.hover, alpha);
	}

	private Map<String, Color> rules(String raw)
	{
		if (raw == null)
		{
			return Collections.emptyMap();
		}
		if (!raw.equals(cachedRulesRaw))
		{
			cachedRulesRaw = raw;
			cachedRules = HighlightRules.parseLookup(raw);
			optionCache.clear();
			targetCache.clear();
		}
		return cachedRules;
	}

	private Shape menuShape(Theme theme, Rectangle bounds)
	{
		if (theme.radius == 0)
		{
			return bounds;
		}
		menuShape.setRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, theme.radius, theme.radius);
		return menuShape;
	}

	private void drawShadow(Graphics2D graphics, Theme theme, Rectangle bounds)
	{
		if (!theme.showShadow)
		{
			return;
		}
		for (int i = SHADOW_LAYERS; i >= 1; i--)
		{
			scratchShape.setRoundRect(bounds.x + i, bounds.y + i, bounds.width, bounds.height, theme.radius, theme.radius);
			graphics.setColor(SHADOW_COLOURS[i - 1]);
			graphics.fill(scratchShape);
		}
	}

	private void drawBorder(Graphics2D graphics, Theme theme, Rectangle bounds)
	{
		if (!theme.showBorder)
		{
			return;
		}
		graphics.setColor(theme.border);
		if (theme.radius > 0)
		{
			scratchShape.setRoundRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1, theme.radius, theme.radius);
			graphics.draw(scratchShape);
		}
		else
		{
			graphics.drawRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);
		}
	}

	private void drawHover(Graphics2D graphics, Theme theme, int x, int rowY, int width, int rowHeight)
	{
		switch (theme.hoverStyle)
		{
			case FILL:
				graphics.setColor(hoverColour);
				graphics.fillRect(x, rowY, width, rowHeight);
				break;
			case BAR:
				graphics.setColor(hoverColour);
				graphics.fillRect(x, rowY, width, rowHeight);
				graphics.setColor(theme.accent);
				graphics.fillRect(x, rowY, HOVER_BAR_WIDTH, rowHeight);
				break;
			case OUTLINE:
				graphics.setColor(theme.accent);
				graphics.drawRect(x + OUTLINE_INSET, rowY + OUTLINE_INSET,
					width - 1 - OUTLINE_INSET * 2, rowHeight - 1 - OUTLINE_INSET * 2);
				break;
			case TEXT:
				// drawn as an accent-coloured option in drawRow
				break;
			case NONE:
			default:
				break;
		}
	}

	private static int centreOffset(int boxHeight, FontMetrics metrics, int nudge)
	{
		return (boxHeight - (metrics.getAscent() + metrics.getDescent())) / 2 + metrics.getAscent() + nudge;
	}

	private static int drawRun(Graphics2D graphics, FontMetrics metrics, String text, int x, int y, Color colour,
		boolean shadow)
	{
		if (text.isEmpty())
		{
			return 0;
		}
		if (shadow)
		{
			graphics.setColor(TEXT_SHADOW_COLOUR);
			graphics.drawString(text, x + TEXT_SHADOW_OFFSET, y + TEXT_SHADOW_OFFSET);
		}
		graphics.setColor(colour);
		graphics.drawString(text, x, y);
		return metrics.stringWidth(text);
	}

	private int drawIcon(Graphics2D graphics, FontMetrics metrics, Client client, int index, int x, int y)
	{
		BufferedImage icon = modIcon(client, index);
		if (icon == null)
		{
			return 0;
		}
		int height = iconHeight(metrics);
		int width = iconWidth(icon, height);
		graphics.drawImage(icon, x, y - height, width, height, null);
		return width + ICON_GAP;
	}

	private static int iconHeight(FontMetrics metrics)
	{
		return Math.max(1, metrics.getAscent());
	}

	private static int iconWidth(BufferedImage icon, int height)
	{
		return Math.max(1, icon.getWidth() * height / icon.getHeight());
	}

	private BufferedImage modIcon(Client client, int index)
	{
		BufferedImage cached = modIconCache.get(index);
		if (cached != null)
		{
			return cached;
		}
		IndexedSprite[] icons = client != null ? client.getModIcons() : null;
		if (icons == null || index < 0 || index >= icons.length || icons[index] == null)
		{
			return null;
		}
		BufferedImage built = buildModIcon(icons[index]);
		if (built != null)
		{
			modIconCache.put(index, built);
		}
		return built;
	}

	private static BufferedImage buildModIcon(IndexedSprite sprite)
	{
		int width = sprite.getWidth();
		int height = sprite.getHeight();
		byte[] pixels = sprite.getPixels();
		int[] palette = sprite.getPalette();
		if (width <= 0 || height <= 0 || pixels == null || palette == null)
		{
			return null;
		}
		int[] argb = new int[width * height];
		for (int i = 0; i < pixels.length && i < argb.length; i++)
		{
			int paletteIndex = pixels[i] & 0xFF;
			argb[i] = paletteIndex == 0 || paletteIndex >= palette.length ? 0 : (0xFF000000 | palette[paletteIndex]);
		}
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		image.setRGB(0, 0, width, height, argb, 0, width);
		return image;
	}

	private static Font orFallback(Font font, Font fallback)
	{
		return font != null ? font : fallback;
	}

	private static Font buildFont(FontChoice choice, int size)
	{
		if (choice.isScalable())
		{
			return new Font(choice.family, Font.PLAIN, size);
		}
		switch (choice)
		{
			case RS_SMALL:
				return FontManager.getRunescapeSmallFont();
			case RS_BOLD:
				return FontManager.getRunescapeBoldFont();
			case RS_REGULAR:
			default:
				return FontManager.getRunescapeFont();
		}
	}

	private static Font buildBoldFont(FontChoice choice, int size)
	{
		return choice.isScalable() ? new Font(choice.family, Font.BOLD, size) : FontManager.getRunescapeBoldFont();
	}

	private OptionSegments optionSegments(String raw)
	{
		String key = clean(raw);
		OptionSegments segments = optionCache.get(key);
		if (segments == null)
		{
			String plain = Text.removeTags(key);
			segments = new OptionSegments(parseRuns(key), plain, plain.toLowerCase(Locale.ROOT).trim());
			optionCache.put(key, segments);
		}
		return segments;
	}

	private List<Run> targetRuns(String raw, boolean hideLevel)
	{
		String key = clean(raw);
		if (hideLevel)
		{
			key = stripLevel(key);
		}
		List<Run> runs = targetCache.get(key);
		if (runs == null)
		{
			runs = new ArrayList<>();
			for (Run run : parseRuns(key))
			{
				splitLevel(run, runs);
			}
			targetCache.put(key, runs);
		}
		return runs;
	}

	private static String clean(String raw)
	{
		return raw == null ? "" : Limits.truncate(raw, MAX_ENTRY_LENGTH);
	}

	/**
	 * Removes any {@code (level-N)} segment and the whitespace it leaves trailing the name.
	 */
	private static String stripLevel(String target)
	{
		Matcher matcher = LEVEL.matcher(target);
		if (!matcher.find())
		{
			return target;
		}
		return TRAILING_SPACE.matcher(matcher.replaceAll("")).replaceAll("");
	}

	/**
	 * Splits on {@code <col=..>}, {@code </col>} and {@code <img=N>} tags. Every other tag (and any
	 * unparseable one) is stripped, so unknown markup never shows up as literal text.
	 */
	private static List<Run> parseRuns(String raw)
	{
		List<Run> runs = new ArrayList<>();
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
			else if (inner.equalsIgnoreCase(CLOSE_COL))
			{
				current = null;
			}
			else if (img.matches())
			{
				runs.add(Run.icon(Integer.parseInt(img.group(1))));
			}
		}
		if (last < raw.length())
		{
			runs.add(new Run(raw.substring(last), current));
		}
		return runs;
	}

	private static void splitLevel(Run run, List<Run> out)
	{
		if (run.iconIndex >= 0)
		{
			out.add(run);
			return;
		}
		Matcher matcher = LEVEL.matcher(run.text);
		int last = 0;
		while (matcher.find())
		{
			if (matcher.start() > last)
			{
				out.add(new Run(run.text.substring(last, matcher.start()), run.tagColour));
			}
			out.add(new Run(matcher.group(), run.tagColour, true, Integer.parseInt(matcher.group(1)), -1));
			last = matcher.end();
		}
		if (last == 0)
		{
			out.add(run);
		}
		else if (last < run.text.length())
		{
			out.add(new Run(run.text.substring(last), run.tagColour));
		}
	}

	private static <V> Map<String, V> boundedCache()
	{
		return new LinkedHashMap<String, V>(SEGMENT_CACHE_SIZE, 0.75f, true)
		{
			@Override
			protected boolean removeEldestEntry(Map.Entry<String, V> eldest)
			{
				return size() > SEGMENT_CACHE_SIZE;
			}
		};
	}

	/**
	 * One row to draw. The overlay builds these from real menu entries, the preview from fixed
	 * samples. {@code kind} drives Custom target colouring.
	 */
	static final class Row
	{
		private String option;
		private String target;
		private TargetKind kind;

		Row(String option, String target, TargetKind kind)
		{
			set(option, target, kind);
		}

		/**
		 * Lets the overlay reuse rows across frames instead of allocating them.
		 */
		void set(String option, String target, TargetKind kind)
		{
			this.option = option;
			this.target = target;
			this.kind = kind;
		}
	}

	enum TargetKind
	{
		NPC,
		PLAYER,
		ITEM,
		OBJECT,
		OTHER
	}

	/**
	 * Target colour settings, read from config once per render rather than once per text run.
	 */
	private static final class TargetColours
	{
		private TargetMode targetMode;
		private CombatLevelMode levelMode;
		private int localLevel;
		private Color npc;
		private Color player;
		private Color item;
		private Color object;
		private Color other;
		private Color lower;
		private Color same;
		private Color higher;

		private void load(BetterRightClickConfig config, int localCombatLevel)
		{
			targetMode = config.targetMode();
			levelMode = config.combatLevelMode();
			localLevel = localCombatLevel;
			npc = config.npcColour();
			player = config.playerColour();
			item = config.itemColour();
			object = config.objectColour();
			other = config.otherColour();
			lower = config.lowerLevelColour();
			same = config.sameLevelColour();
			higher = config.higherLevelColour();
		}

		private Color resolve(Run run, TargetKind kind)
		{
			Color tagColour = run.tagColour != null ? run.tagColour : DEFAULT_TAG_COLOUR;
			if (run.isLevel)
			{
				if (levelMode == CombatLevelMode.CUSTOM)
				{
					return orDefault(run.level < localLevel ? lower : run.level > localLevel ? higher : same, tagColour);
				}
				return tagColour;
			}
			if (targetMode != TargetMode.CUSTOM)
			{
				return tagColour;
			}
			switch (kind)
			{
				case NPC:
					return orDefault(npc, tagColour);
				case PLAYER:
					return orDefault(player, tagColour);
				case ITEM:
					return orDefault(item, tagColour);
				case OBJECT:
					return orDefault(object, tagColour);
				case OTHER:
				default:
					return orDefault(other, tagColour);
			}
		}

		private static Color orDefault(Color colour, Color fallback)
		{
			return colour != null ? colour : fallback;
		}
	}

	private static final class OptionSegments
	{
		private final List<Run> runs;
		private final String plain;
		private final String key;

		private OptionSegments(List<Run> runs, String plain, String key)
		{
			this.runs = runs;
			this.plain = plain;
			this.key = key;
		}
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
