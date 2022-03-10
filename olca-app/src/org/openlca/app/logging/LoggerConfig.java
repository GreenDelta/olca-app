package org.openlca.app.logging;

import ch.qos.logback.classic.Logger;
import org.openlca.app.AppArg;

import com.google.common.base.Objects;

import ch.qos.logback.classic.Level;
import org.slf4j.LoggerFactory;

/**
 * The configuration of the application logging.
 */
public class LoggerConfig {

	public static void setLevel(Level level) {
		var log = Objects.equal(level, Level.ALL)
			? LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
			: LoggerFactory.getLogger("org.openlca");
		if (log instanceof Logger logger) {
			logger.setLevel(level);
			logger.info("Log-level=" + level);
		}
	}

	public static void setUp() {
		Logger rootLogger = Logger.getRootLogger();
		rootLogger.setLevel(Level.WARN);
		setUpOlcaLogger();
	}

	private static void setUpOlcaLogger() {
		Logger logger = Logger.getLogger("org.openlca");
		HtmlLogFile.create(logger);
		logger.addAppender(new PopupAppender());

		addConsoleOutput(logger);

		setLogLevel(logger);
	}

	private static void addConsoleOutput(Logger logger) {
		var appender = Appenders.createConsoleAppender();
		if (appender == null)
			return;
		logger.addAppender(appender);
	}

	private static void setLogLevel(Logger logger) {
		String level = AppArg.LOG_LEVEL.getValue();
		if (level != null) {
			setLevelFromCommandLine(logger, level);
		} else {
			logger.setLevel(LoggerPreference.getLogLevel());
		}
		logger.info("Log-level=" + logger.getLevel());
	}

	private static void setLevelFromCommandLine(Logger logger, String level) {
		if (level.equalsIgnoreCase("all")) {
			logger.setLevel(Level.ALL);
		} else if (level.equalsIgnoreCase("error")) {
			logger.setLevel(Level.ERROR);
		} else if (level.equalsIgnoreCase("info")) {
			logger.setLevel(Level.INFO);
		} else if (level.equalsIgnoreCase("warn")) {
			logger.setLevel(Level.WARN);
		} else {
			logger.setLevel(Level.INFO);
		}
	}
}
