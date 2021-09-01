package org.openlca.app.rcp;

import java.io.PrintStream;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IOConsole;
import org.openlca.app.M;
import org.openlca.app.editors.Editors;
import org.openlca.expressions.Repl;

/**
 * An action that opens a console and starts the formula interpreter REPL in the
 * console.
 */
class FormulaConsoleAction extends Action {

	public FormulaConsoleAction() {
		setId("FormulaConsoleAction");
		setText(M.FormulaInterpreter);
	}

	@Override
	public void run() {
		var manager = ConsolePlugin.getDefault().getConsoleManager();
		if (manager == null)
			return;

		// there are two cases where we need to destroy the console
		// 1) the user quits the interpreter via a command
		// 2) the user closes the console view

		var console = getConsole(manager);
		var page = Editors.getActivePage();
		var viewListener = new ViewListener(page, manager, console);

		var repl = new Repl(
			console.getInputStream(),
			new PrintStream(console.newOutputStream()),
			new PrintStream(console.newOutputStream()));
		repl.onExit(() -> {
			manager.removeConsoles(new IOConsole[]{console});
			if (page != null) {
				page.removePartListener(viewListener);
			}
		});
		new Thread(repl::start).start();
		console.activate();
	}

	private IOConsole getConsole(IConsoleManager manager) {
		var name = M.FormulaInterpreter;
		var consoles = manager.getConsoles();
		if (consoles != null) {
			for (var c : consoles) {
				if (name.equals(c.getName()))
					return (IOConsole) c;
			}
		}
		var console = new IOConsole(name, null);
		manager.addConsoles(new IConsole[]{console});
		return console;
	}

	private static class ViewListener implements IPartListener2 {

		private final IConsoleManager manager;
		private final IWorkbenchPage page;
		private final IOConsole console;

		ViewListener(IWorkbenchPage page,
			IConsoleManager manager,
			IOConsole console) {
			this.manager = manager;
			this.page = page;
			this.console = console;
			if (page != null) {
				page.addPartListener(this);
			}
		}

		@Override
		public void partClosed(IWorkbenchPartReference partRef) {
			var viewId = IConsoleConstants.ID_CONSOLE_VIEW;
			if (!viewId.equals(partRef.getId()))
				return;
			var isActive = false;
			for (var c : manager.getConsoles()) {
				if (console.equals(c)) {
					isActive = true;
					break;
				}
			}
			if (isActive) {
				manager.removeConsoles(new IConsole[]{console});
			}
			if (page != null) {
				page.removePartListener(this);
			}
		}
	}
}
