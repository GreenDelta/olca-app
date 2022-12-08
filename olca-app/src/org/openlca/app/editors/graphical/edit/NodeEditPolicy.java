package org.openlca.app.editors.graphical.edit;

import java.util.Arrays;
import java.util.Objects;

import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.app.editors.graphical.model.commands.CollapseCommand;
import org.openlca.app.editors.graphical.model.commands.ExpandCommand;
import org.openlca.app.editors.graphical.requests.ExpandCollapseRequest;

import static org.openlca.app.editors.graphical.requests.GraphRequestConstants.*;
import static org.openlca.app.tools.graphics.model.Side.INPUT;
import static org.openlca.app.tools.graphics.model.Side.OUTPUT;

public class NodeEditPolicy extends MinMaxComponentEditPolicy {

	@Override
	public Command getCommand(Request request) {
		if (request instanceof ExpandCollapseRequest req) {
			var node = req.getNode();
			var quiet = req.isQuiet();
			if (req.getType() == REQ_EXPAND_OR_COLLAPSE) {
				var type = req.getNode().isExpanded(req.getSide())
					? REQ_COLLAPSE : REQ_EXPAND;
				return getExpansionCommand(node, req.getSide(), type, quiet);
			}
			if (req.getType() == REQ_EXPAND)
				return getExpansionCommand(node, req.getSide(), REQ_EXPAND, quiet);
			if (req.getType() == REQ_COLLAPSE)
				return getExpansionCommand(node, req.getSide(), REQ_COLLAPSE, quiet);
		}
		return super.getCommand(request);
	}

	private Command getExpansionCommand(Node node, int side, String type,
		boolean quiet) {
		var cc = new CompoundCommand();
		var isExpand = Objects.equals(type, REQ_EXPAND);

		if (side == (INPUT | OUTPUT)) {
			for (var s : Arrays.asList(INPUT, OUTPUT))
				if (node.isExpanded(s) && !isExpand)
					cc.add(new CollapseCommand(node, s));
				else if (!node.isExpanded(s) && isExpand)
					cc.add(new ExpandCommand(node, s, quiet));
		} else if ((side == INPUT) || (side == OUTPUT))
			if (node.isExpanded(side) && !isExpand)
				cc.add(new CollapseCommand(node, side));
			else if (!node.isExpanded(side) && isExpand)
				cc.add(new ExpandCommand(node, side, quiet));
		else return null;

		return cc.unwrap();
	}

}
