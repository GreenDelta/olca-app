package org.openlca.app.editors.graphical.edit;

import static org.openlca.app.editors.graphical.requests.GraphRequestConstants.*;

import java.util.Arrays;
import java.util.Objects;

import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.openlca.app.components.graphics.model.Side;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.app.editors.graphical.model.commands.CollapseCommand;
import org.openlca.app.editors.graphical.model.commands.ExpandCommand;
import org.openlca.app.editors.graphical.model.commands.SetProcessGroupCommand;
import org.openlca.app.editors.graphical.requests.ExpandCollapseRequest;

public class NodeEditPolicy extends MinMaxComponentEditPolicy {

	@Override
	public Command getCommand(Request request) {
		if (REQ_SET_PROCESS_GROUP.equals(request.getType())
				&& getHost() instanceof NodeEditPart node) {
			return new SetProcessGroupCommand(node);
		}
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

	private Command getExpansionCommand(Node node, Side side, String type,
																			boolean quiet) {
		var cc = new CompoundCommand();
		var isExpand = Objects.equals(type, REQ_EXPAND);

		if (side == Side.BOTH) {
			for (var s : Arrays.asList(Side.INPUT, Side.OUTPUT)) {
				if (node.isExpanded(s) && !isExpand) {
					cc.add(new CollapseCommand(node, s));
				} else if (!node.isExpanded(s) && isExpand) {
					cc.add(new ExpandCommand(node, s, quiet));
				}
			}
		} else {
			if (node.isExpanded(side) && !isExpand) {
				cc.add(new CollapseCommand(node, side));
			} else if (!node.isExpanded(side) && isExpand) {
				cc.add(new ExpandCommand(node, side, quiet));
			}
		}

		return cc.unwrap();
	}

}
