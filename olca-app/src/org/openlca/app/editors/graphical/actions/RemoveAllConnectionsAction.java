package org.openlca.app.editors.graphical.actions;

import static org.eclipse.gef.RequestConstants.*;

import java.util.IdentityHashMap;

import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.requests.GroupRequest;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.edit.NodeEditPart;
import org.openlca.app.editors.graphical.model.GraphLink;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.MsgBox;
import org.openlca.util.Lists;

public class RemoveAllConnectionsAction extends SelectionAction {

	private final GraphEditor editor;

	public RemoveAllConnectionsAction(GraphEditor part) {
		super(part);
		editor = part;
		setId(GraphActionIds.REMOVE_ALL_CONNECTIONS);
		setText(M.RemoveConnections);
		setImageDescriptor(Icon.LINK.descriptor());
	}

	@Override
	protected boolean calculateEnabled() {
		// we can enable it, when we find a node with at least one link
		var parts = getSelectedObjects();
		if (Lists.isEmpty(parts)) return false;
		for (var o : parts) {
			if (!(o instanceof NodeEditPart part)) continue;
			var model = part.getModel();
			if (model != null && !model.getAllLinks().isEmpty()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void run() {
		var cmd = getCommand();
		if (cmd == null) return;
		if (!cmd.canExecute()) {
			MsgBox.info(M.ConnectionsCannotBeRemoved);
			return;
		}
		execute(cmd);
	}

	private CompoundCommand getCommand() {
		var parts = getSelectedObjects();
		if (Lists.isEmpty(parts))
			return null;
		if (!(editor.getAdapter(GraphicalViewer.class) instanceof GraphicalViewer g))
			return null;

		// create a command that contains a list of delete commands
		// for each selected link
		var cmd = new CompoundCommand();
		cmd.setDebugLabel("Remove links");
		cmd.setLabel(M.RemoveConnections);

		var handled = new IdentityHashMap<GraphLink, Boolean>();

		for (var o : parts) {
			if (!(o instanceof NodeEditPart part)) continue;
			var node = part.getModel();
			if (node == null) continue;

			for (var l : node.getAllLinks()) {

				// check if it is a new graph link
				if (!(l instanceof GraphLink link)) continue;
				var exists = handled.get(link);
				if (exists != null && exists) continue;
				handled.put(link, Boolean.TRUE);

				// request the delete-link command
				var linkPart = g.getEditPartRegistry().get(link);
				if (linkPart == null) continue;
				var req = new GroupRequest(REQ_DELETE);
				var command = linkPart.getCommand(req);
				if (command != null) {
					cmd.add(command);
				}
			}
		}
		return cmd.isEmpty() ? null : cmd;
	}
}
