package com.rightclickstyler;

import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.plaf.basic.BasicHTML;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class RightClickStylerPanelTest
{
	@Test
	public void disablesHtmlBeforeFirstThemeNameRender()
	{
		DefaultListCellRenderer renderer = RightClickStylerPanel.themeListCellRenderer();
		Theme theme = BuiltInThemes.find(BuiltInThemes.DEFAULT_ID)
			.withIdentity("literal", "<html>Rune Ω !?", false);

		Component component = renderer.getListCellRendererComponent(
			new JList<>(), theme, 0, false, false);

		assertEquals("<html>Rune Ω !?", ((JLabel) component).getText());
		assertNull(((JLabel) component).getClientProperty(BasicHTML.propertyKey));
	}
}
