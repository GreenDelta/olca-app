package org.openlca.app.logging;

import java.io.PrintStream;

import org.apache.commons.io.output.TeeOutputStream;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.openlca.app.util.ErrorReporter;

public class Console {

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
		manager.removeConsoles(new IConsole[] { instance.console });
		instance = null;

		System.setOut(sysOut);
		System.setErr(sysErr);
	}

	private Console() {
		console = findOrCreate();
		stream = console.newMessageStream();
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
		manager.addConsoles(new IConsole[] { console });
		return console;
	}

	private void close() {
		if (stream.isClosed())
			return;
		try {
			stream.flush();
			stream.close();
		} catch (Exception e) {
			ErrorReporter.on("Cannot close console stream.", e);
		}
	}
}
