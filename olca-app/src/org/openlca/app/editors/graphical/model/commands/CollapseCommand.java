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
		collapse(host);
		host.setExpanded(side, false);
	}

	private void collapse(Node node) {
		if (node.isCollapsing)
			return;
		node.isCollapsing = true;

		// It is needed to copy the links otherwise we get a concurrent modification
		// exception
		var links = node.getAllLinks().toArray(new Link[0]);

		for (var link : links) {
			var thisNode = side == INPUT
				? link.getTargetNode()
				: link.getSourceNode();
			var otherNode = side == INPUT
				? link.getSourceNode()
				: link.getTargetNode();
			if (!thisNode.equals(node))
				continue;

			link.disconnect();
			collapse(otherNode);
			collapse(otherNode);
			if (otherNode.equals(host)) { // self-loop
				host.setExpanded(side == INPUT ? OUTPUT : INPUT, false);
			}
			if (!otherNode.getAllLinks().isEmpty())
				continue;
			host.getGraph().removeChild(otherNode);
		}
		node.isCollapsing = false;
	}

}
