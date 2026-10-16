package com.rightclickstyler;

import javax.inject.Singleton;

/**
 * Local player details captured on the client thread so the sidebar preview, which paints on the
 * EDT, never has to touch {@code Client}. {@link #UNKNOWN} while logged out or not yet captured.
 */
@Singleton
class LocalPlayerSnapshot
{
	static final int UNKNOWN = -1;

	private volatile int combatLevel = UNKNOWN;
	// Called from the client thread, so it must be safe off the EDT (repaint() is).
	private volatile Runnable onChange;

	int combatLevel()
	{
		return combatLevel;
	}

	void setOnChange(Runnable onChange)
	{
		this.onChange = onChange;
	}

	void set(int level)
	{
		if (level != combatLevel)
		{
			combatLevel = level;
			notifyChange();
		}
	}

	void clear()
	{
		set(UNKNOWN);
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
