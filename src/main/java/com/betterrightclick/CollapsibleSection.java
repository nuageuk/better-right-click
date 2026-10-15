package com.betterrightclick;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.SwingUtil;

/**
 * A collapsible settings section, styled like the sections in RuneLite's plugin config panel.
 */
class CollapsibleSection extends JPanel
{
	// Sizes and the arrow's darkening match ConfigPanel's sections.
	private static final int ARROW_LUMINANCE_OFFSET = -121;
	private static final int TOGGLE_WIDTH = 18;
	private static final int TOGGLE_GAP = 5;
	private static final int HEADER_BOTTOM_PADDING = 3;
	private static final int HEADER_RIGHT_PADDING = 1;
	private static final int BODY_PADDING = 8;
	private static final int ROW_GAP = 5;

	private static final ImageIcon EXPAND_ICON;
	private static final ImageIcon RETRACT_ICON;

	static
	{
		BufferedImage arrow = ImageUtil.loadImageResource(CollapsibleSection.class, "/util/arrow_right.png");
		arrow = ImageUtil.luminanceOffset(arrow, ARROW_LUMINANCE_OFFSET);
		EXPAND_ICON = new ImageIcon(arrow);
		RETRACT_ICON = new ImageIcon(ImageUtil.rotateImage(arrow, Math.PI / 2));
	}

	private final JPanel body = new JPanel();
	private final JButton toggle = new JButton();

	CollapsibleSection(String name, String description, boolean closedByDefault)
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		JPanel header = new JPanel(new BorderLayout());
		header.setBorder(new CompoundBorder(
			new MatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR),
			new EmptyBorder(0, 0, HEADER_BOTTOM_PADDING, HEADER_RIGHT_PADDING)));

		toggle.setPreferredSize(new Dimension(TOGGLE_WIDTH, 0));
		toggle.setBorder(new EmptyBorder(0, 0, 0, TOGGLE_GAP));
		SwingUtil.removeButtonDecorations(toggle);
		header.add(toggle, BorderLayout.WEST);

		JLabel title = new JLabel(name);
		title.setForeground(ColorScheme.BRAND_ORANGE);
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setToolTipText("<html>" + name + ":<br>" + description + "</html>");
		header.add(title, BorderLayout.CENTER);

		body.setLayout(new DynamicGridLayout(0, 1, 0, ROW_GAP));
		body.setBorder(new CompoundBorder(
			new MatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR),
			new EmptyBorder(BODY_PADDING, 0, BODY_PADDING, 0)));
		setOpen(!closedByDefault);

		MouseAdapter toggleListener = new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				setOpen(!body.isVisible());
			}
		};
		toggle.addActionListener(e -> setOpen(!body.isVisible()));
		title.addMouseListener(toggleListener);
		header.addMouseListener(toggleListener);

		add(header);
		add(body);
	}

	private void setOpen(boolean open)
	{
		body.setVisible(open);
		toggle.setIcon(open ? RETRACT_ICON : EXPAND_ICON);
		toggle.setToolTipText(open ? "Retract" : "Expand");
		revalidate();
	}

	void addRow(JComponent row)
	{
		body.add(row);
	}
}
