package com.rightclickstyler;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class RightClickStylerPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(RightClickStylerPlugin.class);
		RuneLite.main(args);
	}
}
