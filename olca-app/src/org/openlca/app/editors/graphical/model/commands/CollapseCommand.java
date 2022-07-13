package org.openlca.app.editors.graphical.model.commands;

import org.eclipse.gef.commands.Command;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.Link;
import org.openlca.app.editors.graphical.model.Node;

import static org.openlca.app.editors.graphical.model.Node.Side.INPUT;
import static org.openlca.app.editors.graphical.model.Node.Side.OUTPUT;

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
		collapse(host, side);
		host.setExpanded(side, false);
	}

	/**
	 * Recursively collapses all the input or output nodes connected to the given
	 * node.
	 * This method does not collapse:
	 *  - the reference node,
	 *  - nodes that are chained to the reference node.
	 */
	private void collapse(Node node, int side) {
		System.out.println("Collapse " + node + " " + (side == INPUT ? "input" : "output"));
		if (node.isCollapsing)
			return;
		node.isCollapsing = true;

		// It is needed to copy the links otherwise we get a concurrent modification
		// exception
		var links = side == INPUT
			? node.getAllTargetConnections().toArray(new Link[0])
			: node.getAllSourceConnections().toArray(new Link[0]);
		System.out.println("Links:");
		for (var link : links) {
			System.out.println("-" + link);
		}

		for (var link : links) {
			System.out.println(" -> " + link);
			var thisNode = side == INPUT
				? link.getTargetNode()
				: link.getSourceNode();
			var otherNode = side == INPUT
				? link.getSourceNode()
				: link.getTargetNode();

			if (!thisNode.equals(node)  // wrong link
				|| otherNode.equals(host))  // double link
				continue;

			if (host != host.getGraph().getReferenceNode()
				&& (otherNode.isChainingReferenceNode(side)
				|| otherNode == host.getGraph().getReferenceNode()))
				continue;

			System.out.println("    disconnecting " + link);

			link.disconnect();
			collapse(otherNode, INPUT);
			collapse(otherNode, OUTPUT);
			if (otherNode.equals(host)) { // self-loop
				host.setExpanded(side == INPUT ? OUTPUT : INPUT, false);
			}
			var linkStream = otherNode.getAllLinks().stream();
			if (!linkStream.filter(l -> !l.isSelfLoop()).toList().isEmpty())
				continue;
			System.out.println("    removing " + otherNode);
			host.getGraph().removeChild(otherNode);
		}
		node.isCollapsing = false;
	}

}
