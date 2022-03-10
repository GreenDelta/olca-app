package org.openlca.app.logging;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.openlca.app.rcp.RcpActivator;
import org.openlca.util.Strings;
import org.slf4j.event.Level;

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
		var store = RcpActivator.getDefault().getPreferenceStore();
		store.setDefault(LOG_CONSOLE, false);
		store.setDefault(LOG_LEVEL, LEVEL_INFO);
	}

	static Level getLogLevel() {
		var store = RcpActivator.getDefault().getPreferenceStore();
		var levelId = store.getString(LOG_LEVEL);
		if (levelId == null)
			return Level.INFO;
		return getLevelForId(levelId);
	}

	private static Level getLevelForId(String levelId) {
		if (Strings.nullOrEmpty(levelId))
			return Level.INFO;
		return switch (levelId) {
			case LEVEL_ALL -> Level.TRACE;
			case LEVEL_WARN -> Level.WARN;
			case LEVEL_ERROR -> Level.ERROR;
			default -> Level.INFO;
		};
	}

	public static boolean getShowConsole() {
		var store = RcpActivator.getDefault().getPreferenceStore();
		return store.getBoolean(LOG_CONSOLE);
	}

}
