package org.openlca.app.editors.graphical.model.commands;

import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.GraphLink;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.app.tools.graphics.model.Link;

import java.util.ArrayList;
import java.util.List;

import static org.openlca.app.tools.graphics.model.Component.CHILDREN_PROP;
import static org.openlca.app.tools.graphics.model.Side.INPUT;
import static org.openlca.app.tools.graphics.model.Side.OUTPUT;

public class CollapseCommand extends Command {

	private final Node host;
	private final int side;

	public CollapseCommand(Node host, int side) {
		this.host = host;
		this.side = side;
		setLabel(M.Collapse);
	}

	@Override
	public boolean canExecute() {
		return host.isExpanded(side)
			&& (side == INPUT || side == OUTPUT)
			&& !host.isOnlyChainingReferenceNode(side);
	}

	@Override
	public boolean canUndo() {
		return false;
	}

	@Override
	public void execute() {
		redo();
	}

	@Override
	public void redo() {
		collapse(host, host, side);
		host.setExpanded(side, false);
		host.getGraph().firePropertyChange(CHILDREN_PROP, null, null);
	}

	/**
	 * Recursively collapses all the input or output nodes connected to the given
	 * node.
	 * This method does not collapse:
	 *  - the reference node,
	 *  - nodes that are chained to the reference node.
	 */
	protected static void collapse(Node root, Node node, int side) {
		if (node.isCollapsing)
			return;
		node.isCollapsing = true;

		// It is needed to copy the links otherwise we get a concurrent modification
		// exception
		var links = side == INPUT
			? node.getAllTargetConnections().toArray(new Link[0])
			: node.getAllSourceConnections().toArray(new Link[0]);

		for (var l : links) {
			if (l instanceof GraphLink link) {
				var thisNode = side == INPUT
						? link.getTargetNode()
						: link.getSourceNode();
				var otherNode = side == INPUT
						? link.getSourceNode()
						: link.getTargetNode();

				if (!thisNode.equals(node)  // wrong link
						|| otherNode.equals(root))  // double link
					continue;

				if (root != root.getGraph().getReferenceNode()
					&& (otherNode.isChainingReferenceNode(side)
					|| otherNode == root.getGraph().getReferenceNode()))
					continue;

				link.disconnect();
				collapse(root, otherNode, INPUT);
				collapse(root, otherNode, OUTPUT);

				if (link.isCloseLoop()) // close loop
					root.setExpanded(side == INPUT ? OUTPUT : INPUT, false);

				var linkStream = otherNode.getAllLinks().stream()
						.map(GraphLink.class::cast);
				if (!linkStream.filter(con -> !con.isCloseLoop()).toList().isEmpty())
					continue;

				root.getGraph().removeChildQuietly(otherNode);
			}
		}
		node.isCollapsing = false;
	}

}
