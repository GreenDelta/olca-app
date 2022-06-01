package org.openlca.app.editors.graph.actions;

import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.internal.InternalImages;
import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.eclipse.osgi.util.NLS;
import org.openlca.app.M;
import org.openlca.app.editors.graph.GraphEditor;
import org.openlca.app.editors.graph.edit.GraphEditPart;

public class LayoutAction extends WorkbenchPartAction {

	public static final String REQ_LAYOUT = "layout";
	private final GraphEditor graphEditor;

	public LayoutAction(GraphEditor part) {
		super(part);
		graphEditor = part;
		setText(NLS.bind(M.LayoutAs, M.Tree));
		setId(ActionIds.LAYOUT_TREE);
		setImageDescriptor(InternalImages.DESC_HORZ_ALIGN_LEFT);
		setDisabledImageDescriptor(InternalImages.DESC_HORZ_ALIGN_LEFT_DIS);
	}

	@Override
	public void run() {
		execute(getCommand());
	}

	@Override
	protected boolean calculateEnabled() {
		var cmd = getCommand();
		if (cmd == null)
			return false;
		return cmd.canExecute();
	}

	private Command getCommand() {
		var request = new Request(REQ_LAYOUT);
		var registry = graphEditor.getGraphicalViewer().getEditPartRegistry();
		var graphEditPart = (GraphEditPart) registry.get(graphEditor.getModel());
		return graphEditPart.getCommand(request);
	}

}
