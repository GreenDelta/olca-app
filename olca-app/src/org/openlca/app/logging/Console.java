package org.openlca.app.logging;

import java.io.PrintStream;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

/**
 * The logger console.
 */
public class Console extends AppenderSkeleton {

	private static Console instance;
	private MessageConsoleStream logStream;
	private MessageConsole console;

	public static void show() {
		if (instance == null)
			instance = new Console();
		instance.console.activate();
		Logger.getLogger("org.openlca").info("Logging on console");
	}

	public static void dispose() {
		if (instance != null) {
			instance.close();
			IConsoleManager manager = ConsolePlugin.getDefault()
					.getConsoleManager();
			manager.removeConsoles(new IConsole[] { instance.console });
			instance.console.destroy();
			instance = null;
		}
	}

	private Console() {
		console = findOrCreateConsole("Logs");
		logStream = console.newMessageStream();
		Logger logger = Logger.getLogger("org.openlca");
		logger.addAppender(this);
	}

	private MessageConsole findOrCreateConsole(String name) {
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager conMan = plugin.getConsoleManager();
		IConsole[] existing = conMan.getConsoles();
		for (int i = 0; i < existing.length; i++)
			if (name.equals(existing[i].getName()))
				return (MessageConsole) existing[i];
		MessageConsole console = new MessageConsole(name, null);
		conMan.addConsoles(new IConsole[] { console });
		return console;
	}

	@Override
	protected void append(LoggingEvent evt) {
		if (logStream.isClosed())
			return;
		String message;
		if (evt.getLevel().toInt() <= Level.DEBUG_INT) {
			LocationInfo info = evt.getLocationInformation();
			message = "" + evt.getLevel().toString()
					+ " [" + DateFormatUtils.format(evt.timeStamp, "HH:mm:ss.SS") + "]"
					+ " @" + info.getClassName() + ">" + info.getMethodName() + ">" + info.getLineNumber()
					+ " - " + evt.getMessage();
		} else {
			message = "" + evt.getLevel().toString()
					+ " - " + evt.getMessage();
		}
		tryPrintMessage(message, evt.getThrowableInformation());
	}

	private void tryPrintMessage(String message,
			ThrowableInformation throwableInformation) {
		try {
			logStream.println(message);
			if (throwableInformation != null) {
				Throwable throwable = throwableInformation.getThrowable();
				if (throwable != null) {
					logStream.println(throwable.getMessage());
					throwable.printStackTrace(new PrintStream(logStream));
				}
			}
		} catch (Exception e) {
			// do nothing
		}
	}

	@Override
	public void close() {
		Logger logger = Logger.getLogger("org.openlca");
		logger.removeAppender(this);
		if (!logStream.isClosed()) {
			try {
				logStream.flush();
				logStream.close();
			} catch (Exception e) {
				logger.error("Cannot close console stream.", e);
			}
		}
	}

	@Override
	public boolean requiresLayout() {
		return false;
	}

}
