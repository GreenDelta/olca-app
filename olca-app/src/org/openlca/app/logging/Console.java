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

public class Console extends AppenderSkeleton {

	private static Console instance;
	private MessageConsoleStream stream;
	private MessageConsole console;

	public static void show() {
		if (instance == null) {
			instance = new Console();
		}
		instance.console.activate();
	}

	public static void dispose() {
		if (instance == null)
			return;
		instance.close();
		IConsoleManager manager = ConsolePlugin.getDefault()
				.getConsoleManager();
		manager.removeConsoles(new IConsole[] { instance.console });
		instance.console.destroy();
		instance = null;
		System.setOut(System.out);
		System.setErr(System.err);
	}

	private Console() {
		console = findOrCreate("openLCA");
		stream = console.newMessageStream();
		Logger logger = Logger.getLogger("org.openlca");
		logger.addAppender(this);

		// link sys.out and sys.err
		PrintStream pout = new PrintStream(stream);
		System.setOut(pout);
		System.setErr(pout);
	}

	private MessageConsole findOrCreate(String name) {
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager conMan = plugin.getConsoleManager();
		IConsole[] existing = conMan.getConsoles();
		for (int i = 0; i < existing.length; i++)
			if (name.equals(existing[i].getName()))
				return (MessageConsole) existing[i];
		MessageConsole console = new MessageConsole(name, null);
		// set the buffer size of the console
		console.setWaterMarks(1000, 50000);
		conMan.addConsoles(new IConsole[] { console });
		return console;
	}

	@Override
	protected void append(LoggingEvent evt) {
		if (stream.isClosed())
			return;
		String message;
		if (evt.getLevel().toInt() <= Level.DEBUG_INT) {
			LocationInfo info = evt.getLocationInformation();
			message = "" + evt.getLevel().toString()
					+ " [" + DateFormatUtils.format(evt.timeStamp, "HH:mm:ss.SS") + "]"
					+ " @" + info.getClassName()
					+ ">" + info.getMethodName()
					+ ">" + info.getLineNumber()
					+ " - " + evt.getMessage();
		} else {
			message = "" + evt.getLevel().toString()
					+ " - " + evt.getMessage();
		}
		tryPrintMessage(message, evt.getThrowableInformation());
	}

	private void tryPrintMessage(String message, ThrowableInformation info) {
		try {
			stream.println(message);
			if (info != null) {
				Throwable throwable = info.getThrowable();
				if (throwable != null) {
					stream.println(throwable.getMessage());
					throwable.printStackTrace(new PrintStream(stream));
				}
			}
		} catch (Exception e) {
		}
	}

	@Override
	public void close() {
		Logger logger = Logger.getLogger("org.openlca");
		logger.removeAppender(this);
		if (!stream.isClosed()) {
			try {
				stream.flush();
				stream.close();
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
