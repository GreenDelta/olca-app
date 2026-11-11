package org.openlca.app.logging;

import static ch.qos.logback.core.CoreConstants.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import org.openlca.app.rcp.Workspace;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.html.HTMLLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;

class HtmlLog {

	/// The number of older log files that are kept in addition to the current
	/// one. One file is written per day.
	private static final int MAX_HISTORY = 3;

	/// Matches the names of the log files, e.g. `log-2026-11-11.html`. Because
	/// of the ISO date format, the alphabetical order of these names is also
	/// their chronological order.
	private static final Pattern FILE_NAME = Pattern.compile(
		"log-\\d{4}-\\d{2}-\\d{2}\\.html");

	private HtmlLog() {
	}

	/// Returns the folder of the log files.
	static File dir() {
		return new File(Workspace.root(), "log");
	}

	/// Returns the log files, the newest file first.
	static List<File> files() {
		var files = dir().listFiles(
			f -> f.isFile() && FILE_NAME.matcher(f.getName()).matches());
		if (files == null)
			return Collections.emptyList();
		return Arrays.stream(files)
			.sorted(Comparator.comparing(File::getName).reversed())
			.toList();
	}

	/// Returns the date part of the name of the given log file, e.g.
	/// `2026-11-11` for `log-2026-11-11.html`.
	static String dateOf(File file) {
		var name = file.getName();
		return FILE_NAME.matcher(name).matches()
			? name.substring(4, name.length() - 5)
			: name;
	}

	/// Deletes the oldest log files, so that at most `MAX_HISTORY + 1` files
	/// are left, like Logback does: the current file plus `MAX_HISTORY` older
	/// files. However, Logback only deletes the files of specific days in the
	/// past (up to about 5 weeks), so files of older days would be never
	/// deleted, e.g. when openLCA was not used for some time.
	private static void deleteOldFiles() {
		var files = files();
		for (int i = MAX_HISTORY + 1; i < files.size(); i++) {
			var file = files.get(i);
			try {
				Files.deleteIfExists(file.toPath());
			} catch (IOException e) {
				var log = LoggerFactory.getLogger(HtmlLog.class);
				log.warn("failed to delete old log file: {}", file, e);
			}
		}
	}

	static RollingFileAppender<ILoggingEvent> createAppender() {

		var factory = LoggerFactory.getILoggerFactory();
		if (!(factory instanceof LoggerContext context))
			return null;

		var logDir = dir();
		if (logDir.exists()) {
			deleteOldFiles();
		} else {
			try {
				Files.createDirectories(logDir.toPath());
			} catch (IOException e) {
				var log = LoggerFactory.getLogger(HtmlLog.class);
				log.error("failed to create log-dir: {}", logDir, e);
				return null;
			}
		}

		var appender = new RollingFileAppender<ILoggingEvent>();
		appender.setContext(context);
		appender.setName("html");

		var policy = new TimeBasedRollingPolicy<ILoggingEvent>();
		policy.setContext(context);
		policy.setFileNamePattern(
			logDir.getAbsolutePath() + "/log-%d{yyyy-MM-dd}.html");
		policy.setMaxHistory(MAX_HISTORY);
		policy.setCleanHistoryOnStart(true);
		policy.setTotalSizeCap(FileSize.valueOf("10MB"));
		policy.setParent(appender);
		policy.start();

		var layout = new HTML5Layout();
		layout.setContext(context);
		layout.setPattern("%d{HH:mm:ss}%level%logger%msg");
		layout.start();

		var encoder = new LayoutWrappingEncoder<ILoggingEvent>();
		encoder.setContext(context);
		encoder.setCharset(StandardCharsets.UTF_8);
		encoder.setLayout(layout);
		encoder.start();

		appender.setRollingPolicy(policy);
		appender.setEncoder(encoder);
		appender.setAppend(true);
		appender.start();

		return appender;
	}

	public static class HTML5Layout extends HTMLLayout {

		@Override
		public String getFileHeader() {
			StringBuilder sbuf = new StringBuilder();
			sbuf.append("<!DOCTYPE html>");
			sbuf.append(LINE_SEPARATOR);
			sbuf.append("<html>");
			sbuf.append(LINE_SEPARATOR);
			sbuf.append("  <head>");
			sbuf.append(LINE_SEPARATOR);
			sbuf.append("    <title>");
			sbuf.append(title);
			sbuf.append("</title>");
			sbuf.append(LINE_SEPARATOR);

			cssBuilder.addCss(sbuf);

			sbuf.append(LINE_SEPARATOR);
			sbuf.append("  </head>");
			sbuf.append(LINE_SEPARATOR);
			sbuf.append("<body>");
			sbuf.append(LINE_SEPARATOR);

			return sbuf.toString();
		}

	}

}
