package com.menuthemes;

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
 * A collapsible settings section, styled to match the native per-plugin config panel
 * (net.runelite.client.plugins.config.ConfigPanel): an arrow toggle, bold orange title, a
 * bottom separator, and a padded body that stacks rows top to bottom.
 */
class CollapsibleSection extends JPanel
{
	private static final ImageIcon EXPAND_ICON;
	private static final ImageIcon RETRACT_ICON;

	static
	{
		BufferedImage arrow = ImageUtil.loadImageResource(CollapsibleSection.class, "/util/arrow_right.png");
		arrow = ImageUtil.luminanceOffset(arrow, -121);
		EXPAND_ICON = new ImageIcon(arrow);
		RETRACT_ICON = new ImageIcon(ImageUtil.rotateImage(arrow, Math.PI / 2));
	}

	private final JPanel body = new JPanel();
	private final JButton toggle;

	CollapsibleSection(String name, String description, boolean closedByDefault)
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		boolean open = !closedByDefault;

		JPanel header = new JPanel(new BorderLayout());
		header.setBorder(new CompoundBorder(
			new MatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR),
			new EmptyBorder(0, 0, 3, 1)));

		toggle = new JButton(open ? RETRACT_ICON : EXPAND_ICON);
		toggle.setPreferredSize(new Dimension(18, 0));
		toggle.setBorder(new EmptyBorder(0, 0, 0, 5));
		toggle.setToolTipText(open ? "Retract" : "Expand");
		SwingUtil.removeButtonDecorations(toggle);
		header.add(toggle, BorderLayout.WEST);

		JLabel title = new JLabel(name);
		title.setForeground(ColorScheme.BRAND_ORANGE);
		title.setFont(FontManager.getRunescapeBoldFont());
		if (description != null && !description.isEmpty())
		{
			title.setToolTipText("<html>" + name + ":<br>" + description + "</html>");
		}
		header.add(title, BorderLayout.CENTER);

		body.setLayout(new DynamicGridLayout(0, 1, 0, 5));
		body.setBorder(new CompoundBorder(
			new MatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR),
			new EmptyBorder(8, 0, 8, 0)));
		body.setVisible(open);

		MouseAdapter toggleListener = new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				toggle();
			}
		};
		toggle.addActionListener(e -> toggle());
		title.addMouseListener(toggleListener);
		header.addMouseListener(toggleListener);

		add(header);
		add(body);
	}

	private void toggle()
	{
		boolean open = !body.isVisible();
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
