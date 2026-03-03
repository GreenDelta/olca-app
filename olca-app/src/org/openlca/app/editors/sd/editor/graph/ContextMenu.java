package org.openlca.app.editors.sd.editor.graph;

import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.actions.ActionFactory;
import org.openlca.app.editors.sd.editor.graph.actions.AddRateAction;

class ContextMenu extends ContextMenuProvider {

	private final ActionRegistry actions;

	public ContextMenu(GraphicalViewer viewer, ActionRegistry actions) {
		super(viewer);
		this.actions = actions;
	}

	@Override
	public void buildContextMenu(IMenuManager menu) {
		menu.add(actions.getAction(AddRateAction.ID));
		menu.add(actions.getAction(ActionFactory.DELETE.getId()));
	}
}
