package com.betterrightclick;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class BetterRightClickPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(BetterRightClickPlugin.class);
		RuneLite.main(args);
	}
}
