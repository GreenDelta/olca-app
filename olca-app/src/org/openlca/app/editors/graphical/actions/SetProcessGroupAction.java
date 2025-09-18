package org.openlca.app.editors.graphical.actions;

import java.util.List;

import org.eclipse.gef.ui.actions.SelectionAction;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.edit.NodeEditPart;
import org.openlca.app.editors.graphical.model.commands.SetProcessGroupCommand;
import org.openlca.app.rcp.images.Icon;

public class SetProcessGroupAction extends SelectionAction {

	public SetProcessGroupAction(GraphEditor editor) {
		super(editor);
		setId(GraphActionIds.SET_PROCESS_GROUP);
		setText("Set analysis group");
		setImageDescriptor(Icon.ANALYSIS_RESULT.descriptor());
	}

	@Override
	protected boolean calculateEnabled() {
		return !getSelectedNodes().isEmpty();
	}

	@Override
	public void run() {
		var nodes = getSelectedNodes();
		if (nodes.isEmpty())
			return;
		var cmd = new SetProcessGroupCommand(nodes);
		cmd.execute();
	}

	private List<NodeEditPart> getSelectedNodes() {
		var objects = getSelectedObjects();
		if (objects == null || objects.isEmpty())
			return List.of();
		return objects.stream()
				.filter(NodeEditPart.class::isInstance)
				.map(NodeEditPart.class::cast)
				.toList();
	}
}
