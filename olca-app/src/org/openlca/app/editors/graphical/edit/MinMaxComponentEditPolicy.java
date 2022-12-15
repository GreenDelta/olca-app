package org.openlca.app.editors.graphical.edit;

import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.MinMaxComponent;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.app.editors.graphical.model.commands.CollapseCommand;
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

		if (!(child instanceof Node node))
			return null;

		// True if the node is chained to the reference Node on one of its side.
		var chained = node.isChainingReferenceNode(INPUT)
				|| node.isChainingReferenceNode(OUTPUT);
		for (var side : Arrays.asList(INPUT, OUTPUT)) {
			if (!node.isChainingReferenceNode(side)) {
				var com = type == MAXIMIZE && chained
						? new ExpandCommand(node, side, false)
						: type == MINIMIZE
						? new CollapseCommand(node, side) : null;
				if (com != null && com.canExecute())
					cc.add(com);
			}
		}

		cc.add(getMinMaxCommand(type));
		return cc.unwrap();
	}

}
