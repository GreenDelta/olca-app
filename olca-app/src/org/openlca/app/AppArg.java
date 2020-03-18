package org.openlca.app;

import java.io.BufferedReader;
import java.io.StringReader;

import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application arguments that can be passed to the application via the command
 * line or the openLCA.ini file.
 */
public enum AppArg {

	/**
	 * olcaLog: a start log level for the application (all, info, warn, or
	 * error).
	 */
	LOG_LEVEL("olcaLog"),

	/**
	 * olcaDataDir: a full path to a directory folder with write permissions
	 * that will be used as workspace directory.
	 */
	DATA_DIR("olcaDataDir"),

	/**
	 * olcaBuild: the openLCA build number which was set in the build.
	 */
	BUILD_NUMBER("olcaBuild"),

	/**
	 * olcaDevMode: has the value true or false (default) and indicates if the
	 * application is running in developer modus or not.
	 */
	DEV_MODE("olcaDevMode");

	private final String key;

	private AppArg(String key) {
		this.key = key;
	}

	/**
	 * Get the value of the argument that was passed to the application. Returns
	 * null if the argument is not set or has no value.
	 */
	public String getValue() {
		return get(key);
	}

	/**
	 * Returns the value of the given argument that was passed into the application.
	 * It returns null if no value was defined for that argument.
	 */
	public static String get(String arg) {
		String text = System.getProperty("eclipse.commands");
		if (text == null)
			return null;
		try {
			StringReader reader = new StringReader(text);
			BufferedReader buffer = new BufferedReader(reader);
			String line = null;
			String param = null;
			while ((line = buffer.readLine()) != null) {
				if (line.startsWith("-")) {
					param = line.substring(1).trim();
				} else if (param != null) {
					if (Strings.nullOrEqual(param, arg))
						return line.trim();
					param = null;
				}
			}
			return null;
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(AppArg.class);
			log.error("Get args failed", e);
			return null;
		}
	}

}
