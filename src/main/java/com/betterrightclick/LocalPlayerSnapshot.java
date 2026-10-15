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
	// Called from the client thread, so it must be safe off the EDT (repaint() is).
	private volatile Runnable onChange;

	Integer combatLevel()
	{
		return combatLevel;
	}

	void setOnChange(Runnable onChange)
	{
		this.onChange = onChange;
	}

	void set(int level)
	{
		Integer current = combatLevel;
		if (current == null || current != level)
		{
			combatLevel = level;
			notifyChange();
		}
	}

	void clear()
	{
		if (combatLevel != null)
		{
			combatLevel = null;
			notifyChange();
		}
	}

	private void notifyChange()
	{
		Runnable listener = onChange;
		if (listener != null)
		{
			listener.run();
		}
	}
}
