package org.openlca.app.editors.graph.actions;

import org.eclipse.gef.NodeEditPart;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.osgi.util.NLS;
import org.openlca.app.M;
import org.openlca.app.editors.graph.GraphEditor;
import org.openlca.app.editors.graph.edit.GraphEditPart;
import org.openlca.app.editors.graph.layouts.TreeLayoutProcessor;
import org.openlca.app.editors.graph.model.Node;

import static org.eclipse.gef.RequestConstants.REQ_MOVE_CHILDREN;

public class LayoutAction extends SelectionAction {

	private final GraphEditor graphEditor;

	public LayoutAction(GraphEditor part) {
		super(part);
		graphEditor = part;
		setText(NLS.bind(M.LayoutAs, M.Tree));
		setId(ActionIds.LAYOUT_TREE);
	}

	@Override
	public void run() {
		execute(getCommand());
	}

	@Override
	protected boolean calculateEnabled() {
		return canPerformAction();
	}

	private boolean canPerformAction() {
		return true;
	}

	private Command getCommand() {
		var registry = graphEditor.getGraphicalViewer().getEditPartRegistry();
		var graphEditPart = (GraphEditPart) registry.get(graphEditor.getModel());

		CompoundCommand cc = new CompoundCommand();
		cc.setLabel(NLS.bind(M.LayoutAs, M.Tree));

		var layoutProcessor = new TreeLayoutProcessor(graphEditor.getModel());
		var mapNodeToMoveDelta = layoutProcessor.getMoveDeltas();

		for (Node node : graphEditor.getModel().getChildren()) {
			var part = (NodeEditPart) registry.get(node);
			var request = new ChangeBoundsRequest(REQ_MOVE_CHILDREN);
			request.setEditParts(part);
			request.setMoveDelta(mapNodeToMoveDelta.get(node));
			cc.add(graphEditPart.getCommand(request));
		}
		return cc;
	}

}
