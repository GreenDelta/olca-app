package org.openlca.app.logging;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.openlca.app.CommandArgument;

import com.google.common.base.Objects;

/**
 * The configuration of the application logging.
 */
public class LoggerConfig {

	public static void setLevel(Level level) {
		Logger logger = Objects.equal(level, Level.ALL) ? Logger
				.getLogger("org.openlca") : Logger.getRootLogger();
		logger.setLevel(level);
		logger.info("Log-level=" + level);
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
		BasicConfigurator.configure();
		ConsoleAppender appender = new ConsoleAppender(new PatternLayout());
		logger.addAppender(appender);
		appender.setTarget(ConsoleAppender.SYSTEM_OUT);
		appender.activateOptions();
	}

	private static void setLogLevel(Logger logger) {
		String level = CommandArgument.LOG_LEVEL.getValue();
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
