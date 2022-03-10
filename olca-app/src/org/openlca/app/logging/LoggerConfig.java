package org.openlca.app.logging;

import org.openlca.app.AppArg;
import org.openlca.util.Strings;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * The configuration of the application logging.
 */
public class LoggerConfig {

	static void setLevel(Level level) {
		if (level == null)
			return;

		var domainLog = LoggerFactory.getLogger("org.openlca");
		if (domainLog instanceof Logger log) {
			log.setLevel(level);
		}
		domainLog.info("set log-level=" + level);

		var rootLog = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		if (rootLog instanceof Logger log) {
			log.setLevel(translateForRoot(level));
		}
	}

	public static void setUp() {
		var root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		if (!(root instanceof Logger log))
			return;

		var html = HtmlLog.createAppender();
		if (html != null) {
			log.addAppender(html);
		}

		var popup = PopupAppender.create();
		if (popup != null) {
			log.addAppender(popup);
		}

		var arg = AppArg.LOG_LEVEL.getValue();
		var level = arg != null
			? levelOf(arg)
			: LoggerPreference.getLogLevel();
		setLevel(level);
	}

	private static Level levelOf(String arg) {
		if (Strings.nullOrEmpty(arg))
			return Level.INFO;
		return switch (arg.toLowerCase()) {
			case "all", "trace" -> Level.ALL;
			case "debug" -> Level.DEBUG;
			case "warn", "warning" -> Level.WARN;
			case "error" -> Level.ERROR;
			default -> Level.INFO;
		};
	}

	/**
	 * Depending on the log-level of the openLCA domain logger,
	 * we hide some details for the root logger.
	 */
	private static Level translateForRoot(Level level) {
		if (level == null)
			return Level.ERROR;
		return switch (level.levelInt) {
			case Level.WARN_INT -> Level.ERROR;
			case Level.INFO_INT -> Level.WARN;
			case Level.DEBUG_INT -> Level.INFO;
			default -> level;
		};
	}
}
