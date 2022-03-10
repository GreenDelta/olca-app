package org.openlca.app.logging;

import ch.qos.logback.classic.Level;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.openlca.app.rcp.RcpActivator;
import org.openlca.util.Strings;

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
		var store = store();
		store.setDefault(LOG_CONSOLE, false);
		store.setDefault(LOG_LEVEL, LEVEL_INFO);
	}

	static Level getLogLevel() {
		var levelId = store().getString(LOG_LEVEL);
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
		return store().getBoolean(LOG_CONSOLE);
	}

	static IPreferenceStore store() {
		return RcpActivator.getDefault().getPreferenceStore();
	}
}
