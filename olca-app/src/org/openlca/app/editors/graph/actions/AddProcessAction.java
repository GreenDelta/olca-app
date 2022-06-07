package org.openlca.app.editors.graph.actions;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.openlca.app.editors.graph.GraphEditor;
import org.openlca.app.editors.graph.model.Graph;
import org.openlca.app.rcp.images.Images;
import org.openlca.core.model.ModelType;

import static org.openlca.app.editors.graph.requests.GraphRequestConstants.REQ_ADD_PROCESS;

public class AddProcessAction extends WorkbenchPartAction {

	private final Graph graph;
	private final GraphEditor editor;

	public AddProcessAction(GraphEditor part) {
		super(part);
		editor = part;
		graph = part.getModel();
		setId(ActionIds.ADD_PROCESS);
		// TODO (francois) NLS.bind does not seem to work.
		//		setText(NLS.bind(M.Add, M.Process));
		setText("Add process");
		setImageDescriptor(Images.descriptor(ModelType.PROCESS));
	}

	@Override
	public void run() {
		execute(getCommand());
	}

	@Override
	protected boolean calculateEnabled() {
		var command = getCommand();
		if (command == null)
			return false;
		return command.canExecute();
	}

	private Command getCommand() {
		var viewer = (GraphicalViewer) getWorkbenchPart().getAdapter(
			GraphicalViewer.class);
		var registry = viewer.getEditPartRegistry();
		var graphEditPart = (EditPart) registry.get(graph);
		if (graphEditPart == null)
			return null;
		return graphEditPart.getCommand(new Request(REQ_ADD_PROCESS));
	}

}
