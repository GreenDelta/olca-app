package org.openlca.app.rcp;

import java.io.PrintStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.openlca.app.Messages;
import org.openlca.expressions.Repl;

/**
 * An action that opens a console and starts the formula interpreter REPL in the
 * console.
 */
class FormulaConsoleAction extends Action {

	public FormulaConsoleAction() {
		setId("FormulaConsoleAction");
		setText(Messages.FormulaInterpreter);
	}

	@Override
	public void run() {
		IOConsole console = findOrCreateConsole(Messages.FormulaInterpreter);
		ConsoleJob job = new ConsoleJob(console);
		job.schedule();
	}

	private IOConsole findOrCreateConsole(String name) {
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager conMan = plugin.getConsoleManager();
		IConsole[] existing = conMan.getConsoles();
		for (int i = 0; i < existing.length; i++)
			if (name.equals(existing[i].getName()))
				return (IOConsole) existing[i];
		IOConsole console = new IOConsole(name, null);
		conMan.addConsoles(new IConsole[] { console });
		return console;
	}

	private class ConsoleJob extends Job {

		private IOConsole console;

		public ConsoleJob(IOConsole console) {
			super(Messages.FormulaInterpreter);
			this.console = console;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			console.activate();
			IOConsoleOutputStream out = console.newOutputStream();
			IOConsoleOutputStream err = console.newOutputStream();
			Repl repl = new Repl(console.getInputStream(),
					new PrintStream(out), new PrintStream(err));
			repl.start();
			return Status.OK_STATUS;
		}

	}

}
