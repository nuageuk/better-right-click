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
import java.util.Objects;
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

	// The game's own menu geometry. Its click rows use these, so ours must too.
	static final int HEADER_HEIGHT = 19;
	static final int ROW_HEIGHT = 15;

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
	// Kept apart from targetCache so hiding combat levels never runs a regex per frame.
	private final Map<String, List<Run>> strippedTargetCache = boundedCache();
	private final RoundRectangle2D.Float menuShape = new RoundRectangle2D.Float();
	private final RoundRectangle2D.Float scratchShape = new RoundRectangle2D.Float();
	private final Rectangle boxBounds = new Rectangle();
	private final TargetColours targetColours = new TargetColours();

	// Derived from the theme, rebuilt only when it changes.
	private Theme theme;
	private Font font;
	private Font boldFont;
	private Color bgColour;
	private Color headerColour;
	private Color hoverColour;

	private String cachedRulesRaw;
	private Map<String, Color> cachedRules = Collections.emptyMap();

	// Set by prepare() for the current call; shared by drawing and measuring.
	private Client client;
	private FontMetrics metrics;
	private FontMetrics highlightMetrics;
	private Map<String, Color> rules;
	private boolean boldHighlights;
	private boolean hideLevel;
	private int textOffsetX;
	// Bumped when anything that affects text width changes.
	private int styleVersion;

	// Menus stay open and unchanged for many frames, so the last width is reused until something changes.
	private int measuredWidth;
	private int measuredRowsVersion = -1;
	private int measuredStyleVersion = -1;

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
			prepare(g, client, theme, config);
			targetColours.load(config, localCombatLevel);
			renderMenu(g, config, rows, bounds, hoverIndex);
		}
		finally
		{
			g.dispose();
		}
	}

	/**
	 * Width of the widest row (and the header) plus horizontal padding, measured the way the rows are
	 * drawn. Used instead of the game's menu width, which adds its own padding and counts hidden levels.
	 */
	int measureWidth(Graphics2D graphics, Client client, Theme theme, BetterRightClickConfig config, List<Row> rows,
		int rowsVersion)
	{
		prepare(graphics, client, theme, config);
		if (rowsVersion == measuredRowsVersion && styleVersion == measuredStyleVersion)
		{
			return measuredWidth;
		}
		int widest = theme.showHeader ? metrics.stringWidth(HEADER_TEXT) : 0;
		for (int i = 0; i < rows.size(); i++)
		{
			widest = Math.max(widest, layoutRow(null, rows.get(i), 0, 0, Integer.MAX_VALUE, false));
		}
		measuredWidth = widest + textOffsetX * 2;
		measuredRowsVersion = rowsVersion;
		measuredStyleVersion = styleVersion;
		return measuredWidth;
	}

	private void prepare(Graphics2D graphics, Client client, Theme theme, BetterRightClickConfig config)
	{
		boolean themeChanged = theme != this.theme;
		if (themeChanged)
		{
			this.theme = theme;
			font = buildFont(theme.font, theme.fontSize);
			boldFont = buildBoldFont(theme.font, theme.fontSize);
			int alpha = (int) Math.round(theme.opacity * MAX_ALPHA / PERCENT);
			bgColour = ColorUtil.colorWithAlpha(theme.bg, alpha);
			headerColour = ColorUtil.colorWithAlpha(theme.header, alpha);
			hoverColour = ColorUtil.colorWithAlpha(theme.hover, alpha);
		}
		this.client = client;
		metrics = graphics.getFontMetrics(font);

		boolean highlights = config.highlightsEnabled();
		boolean bold = highlights && config.highlightsBold();
		Map<String, Color> newRules = highlights ? rules(config.highlightRules()) : Collections.emptyMap();
		boolean hide = config.combatLevelMode() == CombatLevelMode.HIDE;
		int offset = Limits.textOffsetX(config);
		if (themeChanged || bold != boldHighlights || newRules != rules || hide != hideLevel || offset != textOffsetX)
		{
			styleVersion++;
		}
		highlightMetrics = bold ? graphics.getFontMetrics(boldFont) : metrics;
		boldHighlights = bold;
		rules = newRules;
		hideLevel = hide;
		textOffsetX = offset;
	}

	private void renderMenu(Graphics2D graphics, BetterRightClickConfig config, List<Row> rows, Rectangle bounds,
		int hoverIndex)
	{
		int baselineNudge = Limits.baselineNudge(config);

		// Rows always sit on the game's click rows; hiding the header just trims the box above them.
		Rectangle box = bounds;
		if (!theme.showHeader)
		{
			box = boxBounds;
			box.setBounds(bounds.x, bounds.y + HEADER_HEIGHT, bounds.width, bounds.height - HEADER_HEIGHT);
		}

		drawShadow(graphics, box);
		Shape shape = menuShape(box);
		graphics.setColor(bgColour);
		graphics.fill(shape);

		Shape previousClip = graphics.getClip();
		graphics.clip(shape);
		if (theme.showHeader)
		{
			graphics.setColor(headerColour);
			graphics.fillRect(bounds.x, bounds.y, bounds.width, HEADER_HEIGHT);
			graphics.setFont(font);
			drawRun(graphics, metrics, HEADER_TEXT, bounds.x + textOffsetX,
				bounds.y + centreOffset(HEADER_HEIGHT, baselineNudge), theme.headerText);
		}
		drawRows(graphics, config, rows, bounds, hoverIndex, bounds.y + HEADER_HEIGHT, baselineNudge);
		graphics.setClip(previousClip);

		drawBorder(graphics, box);
	}

	private void drawRows(Graphics2D graphics, BetterRightClickConfig config, List<Row> rows, Rectangle bounds,
		int hoverIndex, int firstRowY, int baselineNudge)
	{
		boolean showHitRows = config.showHitRows();
		int rowTextOffsetY = centreOffset(ROW_HEIGHT, baselineNudge);

		for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++)
		{
			int rowY = firstRowY + rowIndex * ROW_HEIGHT;
			boolean hovered = rowIndex == hoverIndex;
			if (hovered)
			{
				drawHover(graphics, bounds.x, rowY, bounds.width);
			}
			if (showHitRows)
			{
				graphics.setColor(HIT_ROW_COLOUR);
				graphics.drawRect(bounds.x, rowY, bounds.width - 1, ROW_HEIGHT - 1);
			}
			layoutRow(graphics, rows.get(rowIndex), bounds.x + textOffsetX, rowY + rowTextOffsetY,
				bounds.x + bounds.width, hovered);
		}
	}

	/**
	 * Draws a row's option and target, or only measures them when {@code graphics} is null.
	 *
	 * @return the x position just after the row's content
	 */
	private int layoutRow(Graphics2D graphics, Row row, int startX, int textY, int maxX, boolean hovered)
	{
		OptionSegments option = optionSegments(row.option);
		Color highlight = rules.get(option.key);
		FontMetrics optionMetrics = highlight != null ? highlightMetrics : metrics;
		if (graphics != null)
		{
			graphics.setFont(optionMetrics.getFont());
		}

		int x = startX;
		if (hovered && theme.hoverStyle == HoverStyle.TEXT)
		{
			x += drawRun(graphics, optionMetrics, option.plain, x, textY, theme.accent);
		}
		else if (highlight != null)
		{
			x += drawRun(graphics, optionMetrics, option.plain, x, textY, highlight);
		}
		else
		{
			List<Run> runs = option.runs;
			for (int i = 0; i < runs.size(); i++)
			{
				if (x >= maxX)
				{
					return x;
				}
				Run run = runs.get(i);
				x += run.iconIndex >= 0
					? drawIcon(graphics, optionMetrics, run.iconIndex, x, textY)
					: drawRun(graphics, optionMetrics, run.text, x, textY,
						run.tagColour != null ? run.tagColour : theme.text);
			}
		}

		if (row.target == null || row.target.isEmpty())
		{
			return x;
		}
		if (graphics != null)
		{
			graphics.setFont(font);
		}
		x += metrics.charWidth(' ');
		List<Run> runs = targetRuns(row.target);
		for (int i = 0; i < runs.size(); i++)
		{
			if (x >= maxX)
			{
				return x;
			}
			Run run = runs.get(i);
			if (run.iconIndex >= 0)
			{
				x += drawIcon(graphics, metrics, run.iconIndex, x, textY);
				continue;
			}
			Color colour = graphics != null ? targetColours.resolve(run, row.kind) : null;
			x += drawRun(graphics, metrics, run.text, x, textY, colour);
		}
		return x;
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
		}
		return cachedRules;
	}

	private Shape menuShape(Rectangle bounds)
	{
		if (theme.radius == 0)
		{
			return bounds;
		}
		menuShape.setRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, theme.radius, theme.radius);
		return menuShape;
	}

	private void drawShadow(Graphics2D graphics, Rectangle bounds)
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

	private void drawBorder(Graphics2D graphics, Rectangle bounds)
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

	private void drawHover(Graphics2D graphics, int x, int rowY, int width)
	{
		switch (theme.hoverStyle)
		{
			case FILL:
				graphics.setColor(hoverColour);
				graphics.fillRect(x, rowY, width, ROW_HEIGHT);
				break;
			case BAR:
				graphics.setColor(hoverColour);
				graphics.fillRect(x, rowY, width, ROW_HEIGHT);
				graphics.setColor(theme.accent);
				graphics.fillRect(x, rowY, HOVER_BAR_WIDTH, ROW_HEIGHT);
				break;
			case OUTLINE:
				graphics.setColor(theme.accent);
				graphics.drawRect(x + OUTLINE_INSET, rowY + OUTLINE_INSET,
					width - 1 - OUTLINE_INSET * 2, ROW_HEIGHT - 1 - OUTLINE_INSET * 2);
				break;
			case TEXT:
				// drawn as an accent-coloured option in layoutRow
				break;
			case NONE:
			default:
				break;
		}
	}

	private int centreOffset(int boxHeight, int nudge)
	{
		return (boxHeight - (metrics.getAscent() + metrics.getDescent())) / 2 + metrics.getAscent() + nudge;
	}

	/**
	 * @return the run's width; nothing is drawn when {@code graphics} is null
	 */
	private int drawRun(Graphics2D graphics, FontMetrics fontMetrics, String text, int x, int y, Color colour)
	{
		if (text.isEmpty())
		{
			return 0;
		}
		if (graphics != null)
		{
			if (theme.textShadow)
			{
				graphics.setColor(TEXT_SHADOW_COLOUR);
				graphics.drawString(text, x + TEXT_SHADOW_OFFSET, y + TEXT_SHADOW_OFFSET);
			}
			graphics.setColor(colour);
			graphics.drawString(text, x, y);
		}
		return fontMetrics.stringWidth(text);
	}

	/**
	 * @return the icon's width plus spacing; nothing is drawn when {@code graphics} is null
	 */
	private int drawIcon(Graphics2D graphics, FontMetrics fontMetrics, int index, int x, int y)
	{
		BufferedImage icon = modIcon(index);
		if (icon == null)
		{
			return 0;
		}
		int height = Math.max(1, fontMetrics.getAscent());
		int width = Math.max(1, icon.getWidth() * height / icon.getHeight());
		if (graphics != null)
		{
			graphics.drawImage(icon, x, y - height, width, height, null);
		}
		return width + ICON_GAP;
	}

	private BufferedImage modIcon(int index)
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

	private List<Run> targetRuns(String raw)
	{
		String key = clean(raw);
		Map<String, List<Run>> cache = hideLevel ? strippedTargetCache : targetCache;
		List<Run> runs = cache.get(key);
		if (runs == null)
		{
			runs = new ArrayList<>();
			for (Run run : parseRuns(hideLevel ? stripLevel(key) : key))
			{
				splitLevel(run, runs);
			}
			cache.put(key, runs);
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
		 *
		 * @return true if the text changed, which is all that affects the row's width
		 */
		boolean set(String option, String target, TargetKind kind)
		{
			boolean changed = !Objects.equals(this.option, option) || !Objects.equals(this.target, target);
			this.option = option;
			this.target = target;
			this.kind = kind;
			return changed;
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
