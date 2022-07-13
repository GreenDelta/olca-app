package org.openlca.app.editors.graphical.edit;

import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.MinMaxGraphComponent;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.app.editors.graphical.model.commands.ExpandCommand;
import org.openlca.app.editors.graphical.model.commands.MinMaxCommand;

import java.util.Arrays;

import static org.openlca.app.editors.graphical.model.Node.Side.INPUT;
import static org.openlca.app.editors.graphical.model.Node.Side.OUTPUT;
import static org.openlca.app.editors.graphical.model.commands.MinMaxCommand.MAXIMIZE;
import static org.openlca.app.editors.graphical.model.commands.MinMaxCommand.MINIMIZE;
import static org.openlca.app.editors.graphical.requests.GraphRequestConstants.REQ_MAX;
import static org.openlca.app.editors.graphical.requests.GraphRequestConstants.REQ_MIN;

public class MinMaxComponentEditPolicy extends GraphComponentEditPolicy {

	@Override
	public Command getCommand(Request request) {
		if (REQ_OPEN.equals(request.getType())) {
			var child = (MinMaxGraphComponent) getHost().getModel();
			return getOpenCommand(child.isMinimized() ? MAXIMIZE : MINIMIZE);
		}
		if (REQ_MIN.equals(request.getType())) {
			var child = (MinMaxGraphComponent) getHost().getModel();
			if (!child.isMinimized())
				return getMinMaxCommand(MINIMIZE);
		}
		if (REQ_MAX.equals(request.getType())) {
			var child = (MinMaxGraphComponent) getHost().getModel();
			if (child.isMinimized())
				return getMinMaxCommand(MAXIMIZE);
		}
		return super.getCommand(request);
	}

	private Command getMinMaxCommand(int type) {
		var childEditPart = getHost();
		var child = (MinMaxGraphComponent) childEditPart.getModel();
		var command = new MinMaxCommand(type);
		command.setChild(child);
		return command;
	}

	private Command getOpenCommand(int type) {
		var childEditPart = getHost();
		var child = (MinMaxGraphComponent) childEditPart.getModel();

		var cc = new CompoundCommand();
		cc.setLabel(M.Open);

		if (child instanceof Node node && type == MAXIMIZE) {
			for (var side : Arrays.asList(INPUT, OUTPUT)) {
				var com = new ExpandCommand(node, side);
				if (com.canExecute())
					cc.add(com);
			}
		}

		cc.add(getMinMaxCommand(type));
		return cc.unwrap();
	}

}
