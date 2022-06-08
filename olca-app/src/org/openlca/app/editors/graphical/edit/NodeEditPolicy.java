package org.openlca.app.editors.graphical.edit;

import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.app.editors.graphical.model.commands.ExpansionCommand;
import org.openlca.app.editors.graphical.requests.ExpansionRequest;

import java.util.Objects;

import static org.openlca.app.editors.graphical.model.Node.Side.INPUT;
import static org.openlca.app.editors.graphical.model.Node.Side.OUTPUT;
import static org.openlca.app.editors.graphical.requests.GraphRequestConstants.*;

public class NodeEditPolicy extends MinMaxComponentEditPolicy {

	@Override
	public Command getCommand(Request request) {
		if (request instanceof ExpansionRequest req) {
			if (req.getType() == REQ_EXPAND_OR_COLLAPSE) {
				var type = req.getNode().isExpanded(req.getSide())
					? REQ_COLLAPSE : REQ_EXPAND;
				return getExpansionCommand(req.getNode(), req.getSide(), type);
			}
			if (req.getType() == REQ_EXPAND)
				return getExpansionCommand(req.getNode(), req.getSide(), REQ_EXPAND);
			if (req.getType() == REQ_COLLAPSE)
				return getExpansionCommand(req.getNode(), req.getSide(), REQ_COLLAPSE);
		}
		return super.getCommand(request);
	}

	private Command getExpansionCommand(Node node, int side, String type) {
		var cc = new CompoundCommand();
		var isExpand = Objects.equals(type, REQ_EXPAND);

		if (side == (INPUT | OUTPUT)) {
			if (node.isExpanded(INPUT) != isExpand)
				cc.add(new ExpansionCommand(node, INPUT, isExpand));
			if (node.isExpanded(OUTPUT) != isExpand)
				cc.add(new ExpansionCommand(node, OUTPUT, isExpand));
		} else if ((side == INPUT) || (side == OUTPUT))
			if (node.isExpanded(side) != isExpand)
				cc.add(new ExpansionCommand(node, side, isExpand));
		else return null;

		return cc.unwrap();
	}

}
