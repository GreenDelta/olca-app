package org.openlca.app.editors.sd.editor.graph.model.commands;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.gef.commands.Command;
import org.openlca.app.editors.sd.editor.graph.model.SdGraph;
import org.openlca.app.editors.sd.editor.graph.model.SdLink;
import org.openlca.app.editors.sd.editor.graph.model.SdNode;

/**
 * Command to delete a node from the graph.
 */
public class DeleteNodeCommand extends Command {

	private final SdGraph graph;
	private final SdNode node;
	private List<SdLink> removedLinks;

	public DeleteNodeCommand(SdGraph graph, SdNode node) {
		this.graph = graph;
		this.node = node;
		setLabel("Delete " + node.getDisplayName());
	}

	@Override
	public boolean canExecute() {
		return graph != null && node != null;
	}

	@Override
	public void execute() {
		// Store links for undo
		removedLinks = new ArrayList<>();
		for (var link : node.getAllConnections()) {
			if (link instanceof SdLink sdLink) {
				removedLinks.add(sdLink);
				sdLink.disconnect();
			}
		}

		graph.removeChild(node);

		// TODO: Sync with the actual SD model
		// This would remove the corresponding variable from the XMILE model
	}

	@Override
	public void undo() {
		graph.addChild(node);

		// Restore links
		for (var link : removedLinks) {
			link.reconnect();
		}

		// TODO: Restore in the actual SD model
	}

	@Override
	public boolean canUndo() {
		return graph != null && node != null;
	}
}
