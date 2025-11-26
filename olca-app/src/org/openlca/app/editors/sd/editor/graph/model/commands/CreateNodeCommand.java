package org.openlca.app.editors.sd.editor.graph.model.commands;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.openlca.app.editors.sd.editor.graph.model.SdGraph;
import org.openlca.app.editors.sd.editor.graph.model.SdNode;

/**
 * Command to create a new node in the graph.
 */
public class CreateNodeCommand extends Command {

	private final SdGraph graph;
	private final SdNode node;
	private final Rectangle bounds;

	public CreateNodeCommand(SdGraph graph, SdNode node, Rectangle bounds) {
		this.graph = graph;
		this.node = node;
		this.bounds = bounds;
		setLabel("Create " + node.getType().name().toLowerCase());
	}

	@Override
	public boolean canExecute() {
		return graph != null && node != null;
	}

	@Override
	public void execute() {
		if (bounds != null) {
			node.setLocation(bounds.getLocation());
			if (bounds.width > 0 && bounds.height > 0) {
				node.setSize(bounds.getSize());
			}
		}
		graph.addNode(node);

		// TODO: Sync with the actual SD model
		// This would create the corresponding variable in the XMILE model
	}

	@Override
	public void undo() {
		graph.removeNode(node);

		// TODO: Remove from the actual SD model
	}

	@Override
	public boolean canUndo() {
		return graph != null && node != null;
	}
}
