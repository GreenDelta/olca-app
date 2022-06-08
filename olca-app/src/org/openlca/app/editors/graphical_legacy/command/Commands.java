package org.openlca.app.editors.graphical_legacy.command;

import org.eclipse.gef.commands.Command;
import org.openlca.app.editors.graphical_legacy.GraphEditor;

public class Commands {

	public static Command chain(Command command, Command toChain) {
		if (command == null)
			return toChain;
		return command.chain(toChain);
	}

	public static void executeCommand(Command command, GraphEditor editor) {
		if (command == null)
			return;
		editor.getCommandStack().execute(command);

	}

}
