package org.openlca.app.editors.sd.editor.graph.model.commands;

import org.eclipse.gef.commands.Command;
import org.openlca.app.editors.sd.editor.graph.model.SdLink;
import org.openlca.app.editors.sd.editor.graph.model.SdNode;

/**
 * Command to delete a link.
 */
public class DeleteLinkCommand extends Command {

	private final SdLink link;
	private SdNode source;
	private SdNode target;
	private boolean wasFlowConnection;

	public DeleteLinkCommand(SdLink link) {
		this.link = link;
		setLabel("Delete connection");
	}

	@Override
	public boolean canExecute() {
		return link != null;
	}

	@Override
	public void execute() {
		// Store for undo
		source = link.getSourceNode();
		target = link.getTargetNode();
		wasFlowConnection = link.isFlowConnection();

		link.disconnect();

		// TODO: Sync with the actual SD model
		// This would remove the dependency between variables in the XMILE model
	}

	@Override
	public void undo() {
		if (source != null && target != null) {
			link.reconnect(source, target);
		}

		// TODO: Restore in the actual SD model
	}

	@Override
	public boolean canUndo() {
		return source != null && target != null;
	}
}
