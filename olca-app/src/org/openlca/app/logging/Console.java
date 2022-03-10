package org.openlca.app.logging;

import java.io.PrintStream;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.AppenderBase;
import org.apache.commons.io.output.TeeOutputStream;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.openlca.app.util.ErrorReporter;

public class Console  extends AppenderBase<ILoggingEvent> {

	private static final PrintStream sysOut = System.out;
	private static final PrintStream sysErr = System.err;
	private static Console instance;
	private final MessageConsoleStream stream;
	private final MessageConsole console;


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
		var manager = ConsolePlugin.getDefault().getConsoleManager();
		manager.removeConsoles(new IConsole[]{instance.console});
		instance = null;

		System.setOut(sysOut);
		System.setErr(sysErr);
	}

	private Console() {
		console = findOrCreate();
		stream = console.newMessageStream();
		// var logger = Logger.getLogger("org.openlca");
		// logger.addAppender(this);

		// link sys.out and sys.err
		var teeOut = new TeeOutputStream(sysOut, stream);
		System.setOut(new PrintStream(teeOut));
		var teeErr = new TeeOutputStream(sysErr, stream);
		System.setErr(new PrintStream(teeErr));
	}

	private MessageConsole findOrCreate() {
		var plugin = ConsolePlugin.getDefault();
		var manager = plugin.getConsoleManager();
		var consoles = manager.getConsoles();
		if (consoles != null) {
			for (var c : consoles) {
				if ("openLCA".equals(c.getName()))
					return (MessageConsole) c;
			}
		}
		var console = new MessageConsole("openLCA", null);
		// set the buffer size of the console
		console.setWaterMarks(1000, 50000);
		manager.addConsoles(new IConsole[]{console});
		return console;
	}

	@Override
	protected void append(ILoggingEvent evt) {
		if (stream.isClosed())
			return;
		String message = evt.getLevel()	+ " - " + evt.getMessage();
		try {
			stream.println(message);
			if (evt.getThrowableProxy() instanceof ThrowableProxy tp) {
				var throwable = tp.getThrowable();
				if (throwable != null) {
					stream.println(throwable.getMessage());
					throwable.printStackTrace(new PrintStream(stream));
				}
			}
		} catch (Exception ignored) {
		}
	}

	public void close() {
		// Logger logger = Logger.getLogger("org.openlca");
		// logger.removeAppender(this);
		if (!stream.isClosed()) {
			try {
				stream.flush();
				stream.close();
			} catch (Exception e) {
				ErrorReporter.on("Cannot close console stream.", e);
			}
		}
	}
}
