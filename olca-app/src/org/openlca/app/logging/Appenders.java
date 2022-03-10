package org.openlca.app.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.joran.spi.ConsoleTarget;
import org.slf4j.LoggerFactory;

class Appenders {

	private Appenders() {
	}

	static ConsoleAppender<ILoggingEvent> createConsoleAppender() {
		var factory = LoggerFactory.getILoggerFactory();
		if (!(factory instanceof LoggerContext context))
			return null;

		var encoder = new PatternLayoutEncoder();
		encoder.setContext(context);
		encoder.setPattern(
			"%d{HH:mm:ss.SSS} %green([%thread]) %highlight(%level) %logger{50} - %msg%n\"");
		encoder.start();

		var appender = new ConsoleAppender<ILoggingEvent>();
		appender.setContext(context);
		appender.setName("console");
		appender.setEncoder(encoder);
		appender.setTarget(ConsoleTarget.SystemOut.getName());
		appender.start();
		return appender;
	}

}
