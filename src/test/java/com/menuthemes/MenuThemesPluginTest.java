package com.menuthemes;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class MenuThemesPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(MenuThemesPlugin.class);
		RuneLite.main(args);
	}
}
