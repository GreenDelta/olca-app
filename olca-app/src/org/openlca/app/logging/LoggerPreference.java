package org.openlca.app.logging;

import org.apache.log4j.Level;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.openlca.app.rcp.RcpActivator;

/**
 * The preferences of the application logging.
 */
public class LoggerPreference extends AbstractPreferenceInitializer {

	public static final String LOG_LEVEL = "olca-log-level";
	public static final String LOG_CONSOLE = "olca-log-console";

	public static final String LEVEL_ALL = "olca-log-level-all";
	public static final String LEVEL_INFO = "olca-log-level-info";
	public static final String LEVEL_WARN = "olca-log-level-warn";
	public static final String LEVEL_ERROR = "olca-log-level-error";

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = getStore();
		store.setDefault(LOG_CONSOLE, false);
		store.setDefault(LOG_LEVEL, LEVEL_INFO);
	}

	static Level getLogLevel() {
		IPreferenceStore store = getStore();
		String levelId = store.getString(LOG_LEVEL);
		if (levelId == null)
			return Level.INFO;
		return getLevelForId(levelId);
	}

	private static Level getLevelForId(String levelId) {
		switch (levelId) {
		case LEVEL_ALL:
			return Level.ALL;
		case LEVEL_INFO:
			return Level.INFO;
		case LEVEL_WARN:
			return Level.WARN;
		case LEVEL_ERROR:
			return Level.ERROR;
		}
		return Level.INFO;
	}

	public static boolean getShowConsole() {
		IPreferenceStore store = getStore();
		return store.getBoolean(LOG_CONSOLE);
	}

	static IPreferenceStore getStore() {
		return RcpActivator.getDefault().getPreferenceStore();
	}

}
