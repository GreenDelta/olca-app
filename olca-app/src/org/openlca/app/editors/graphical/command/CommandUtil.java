package org.openlca.app.editors.graphical.command;

import org.eclipse.gef.commands.Command;
import org.openlca.app.editors.graphical.ProductSystemGraphEditor;

public class CommandUtil {

	public static Command chain(Command command, Command toChain) {
		if (command == null)
			return toChain;
		return command.chain(toChain);
	}

	public static void executeCommand(Command command,
			ProductSystemGraphEditor editor) {
		if (command == null)
			return;
		editor.getCommandStack().execute(command);

	}

}