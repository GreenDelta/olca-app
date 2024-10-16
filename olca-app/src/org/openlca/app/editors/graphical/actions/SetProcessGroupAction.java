package org.openlca.app.editors.graphical.actions;

import org.eclipse.gef.Request;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.edit.NodeEditPart;
import org.openlca.app.editors.graphical.requests.GraphRequestConstants;

public class SetProcessGroupAction extends SelectionAction {

	public SetProcessGroupAction(GraphEditor editor) {
		super(editor);
		setId(GraphActionIds.SET_PROCESS_GROUP);
		setText("Set analysis group");
	}

	@Override
	protected boolean calculateEnabled() {
		return getSelectedNode() != null;
	}

	@Override
	public void run() {
		var node = getSelectedNode();
		if (node == null)
			return;
		var cmd = node.getCommand(new Request(
				GraphRequestConstants.REQ_SET_PROCESS_GROUP));
		if (cmd != null) {
			cmd.execute();
		}
	}

	private NodeEditPart getSelectedNode() {
		var objects = getSelectedObjects();
		if (objects == null || objects.size() != 1)
			return null;
		var obj = objects.get(0);
		return obj instanceof NodeEditPart node
				? node
				: null;
	}
}
