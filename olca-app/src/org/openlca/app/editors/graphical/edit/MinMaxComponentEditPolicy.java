package org.openlca.app.editors.graphical.edit;

import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.MinMaxComponent;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.app.editors.graphical.model.commands.ExpandCommand;
import org.openlca.app.editors.graphical.model.commands.MinMaxCommand;

import java.util.Arrays;

import static org.openlca.app.editors.graphical.model.commands.MinMaxCommand.MAXIMIZE;
import static org.openlca.app.editors.graphical.model.commands.MinMaxCommand.MINIMIZE;
import static org.openlca.app.editors.graphical.requests.GraphRequestConstants.REQ_MAX;
import static org.openlca.app.editors.graphical.requests.GraphRequestConstants.REQ_MIN;
import static org.openlca.app.tools.graphics.model.Side.INPUT;
import static org.openlca.app.tools.graphics.model.Side.OUTPUT;

public class MinMaxComponentEditPolicy extends GraphComponentEditPolicy {

	@Override
	public Command getCommand(Request request) {
		if (REQ_OPEN.equals(request.getType())) {
			var child = (MinMaxComponent) getHost().getModel();
			return getOpenCommand(child.isMinimized() ? MAXIMIZE : MINIMIZE);
		}
		if (REQ_MIN.equals(request.getType())) {
			var child = (MinMaxComponent) getHost().getModel();
			if (!child.isMinimized())
				return getMinMaxCommand(MINIMIZE);
		}
		if (REQ_MAX.equals(request.getType())) {
			var child = (MinMaxComponent) getHost().getModel();
			if (child.isMinimized())
				return getMinMaxCommand(MAXIMIZE);
		}
		return super.getCommand(request);
	}

	private Command getMinMaxCommand(int type) {
		var childEditPart = getHost();
		var child = (MinMaxComponent) childEditPart.getModel();
		var command = new MinMaxCommand(type);
		command.setChild(child);
		return command;
	}

	private Command getOpenCommand(int type) {
		var childEditPart = getHost();
		var child = (MinMaxComponent) childEditPart.getModel();

		var cc = new CompoundCommand();
		cc.setLabel(M.Open);

		if (child instanceof Node node && type == MAXIMIZE) {
			for (var side : Arrays.asList(INPUT, OUTPUT)) {
				var com = new ExpandCommand(node, side, false);
				if (com.canExecute())
					cc.add(com);
			}
		}

		cc.add(getMinMaxCommand(type));
		return cc.unwrap();
	}

}
