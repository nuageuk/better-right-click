package com.betterrightclick;

import javax.inject.Singleton;

/**
 * Local player details captured on the client thread so the sidebar preview, which paints on the
 * EDT, never has to touch {@code Client}. Null while logged out or not yet captured.
 */
@Singleton
class LocalPlayerSnapshot
{
	private volatile Integer combatLevel;

	Integer combatLevel()
	{
		return combatLevel;
	}

	void set(int level)
	{
		combatLevel = level;
	}

	void clear()
	{
		combatLevel = null;
	}
}
