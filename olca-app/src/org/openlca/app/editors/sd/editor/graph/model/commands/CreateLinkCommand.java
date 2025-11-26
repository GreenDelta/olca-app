package org.openlca.app.editors.sd.editor.graph.model.commands;

import org.eclipse.gef.commands.Command;
import org.openlca.app.editors.sd.editor.graph.model.SdLink;
import org.openlca.app.editors.sd.editor.graph.model.SdNode;

/**
 * Command to create a link between two nodes.
 */
public class CreateLinkCommand extends Command {

	private final SdNode source;
	private SdNode target;
	private SdLink link;
	private boolean isFlowConnection = false;

	public CreateLinkCommand(SdNode source) {
		this.source = source;
		setLabel("Create connection");
	}

	public void setTarget(SdNode target) {
		this.target = target;
	}

	public void setFlowConnection(boolean flowConnection) {
		this.isFlowConnection = flowConnection;
	}

	@Override
	public boolean canExecute() {
		if (source == null || target == null) {
			return false;
		}
		// Don't allow self-connections
		if (source.equals(target)) {
			return false;
		}
		// Check if connection already exists
		for (var link : source.getSourceConnections()) {
			if (link instanceof SdLink sdLink) {
				if (sdLink.getTarget().equals(target)) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public void execute() {
		link = new SdLink(source, target, isFlowConnection);
		link.connect();

		// TODO: Sync with the actual SD model
		// This would create a dependency between variables in the XMILE model
	}

	@Override
	public void undo() {
		if (link != null) {
			link.disconnect();
		}

		// TODO: Remove from the actual SD model
	}

	@Override
	public boolean canUndo() {
		return link != null;
	}
}
