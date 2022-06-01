package org.openlca.app.editors.graph.actions;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.openlca.app.editors.graph.GraphEditor;
import org.openlca.app.editors.graph.model.Graph;
import org.openlca.app.rcp.images.Images;
import org.openlca.core.model.ModelType;

public class AddProcessAction extends WorkbenchPartAction {

	public static final String REQ_ADD_PROCESS = "add_process";

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
		return getCommand().canExecute();
	}

	private Command getCommand() {
		var registry = editor.getGraphicalViewer().getEditPartRegistry();
		var graphEditPart = (EditPart) registry.get(graph);
		return graphEditPart.getCommand(new Request(REQ_ADD_PROCESS));
	}

}
