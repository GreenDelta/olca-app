package org.openlca.app.results.analysis.sankey.actions;

import static org.openlca.app.editors.graphical.requests.GraphRequestConstants.REQ_LAYOUT;

import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.results.analysis.sankey.SankeyEditor;
import org.openlca.app.results.analysis.sankey.edit.DiagramEditPart;
import org.openlca.app.components.graphics.actions.ActionIds;

public class LayoutAction extends WorkbenchPartAction {

	private final SankeyEditor editor;
	private Command command;

	public LayoutAction(SankeyEditor part) {
		super(part);
		editor = part;
		setText(M.LayoutAsTree);
		setId(ActionIds.LAYOUT_TREE);
		setImageDescriptor(Icon.LAYOUT.descriptor());
	}

	@Override
	public void run() {
		execute(command);
	}

	@Override
	protected boolean calculateEnabled() {
		command = getCommand();
		if (command == null)
			return false;
		return command.canExecute();
	}

	private Command getCommand() {
		var request = new Request(REQ_LAYOUT);
		var viewer = (GraphicalViewer) getWorkbenchPart().getAdapter(
			GraphicalViewer.class);
		var registry = viewer.getEditPartRegistry();
		var graphEditPart = (DiagramEditPart) registry.get(editor.getModel());
		return graphEditPart.getCommand(request);
	}

}
