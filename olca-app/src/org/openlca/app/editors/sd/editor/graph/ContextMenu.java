package org.openlca.app.editors.sd.editor.graph;

import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.actions.ActionFactory;
import org.openlca.app.editors.sd.editor.graph.actions.AddAuxiliaryAction;
import org.openlca.app.editors.sd.editor.graph.actions.AddRateAction;
import org.openlca.app.editors.sd.editor.graph.actions.AddStockAction;

class ContextMenu extends ContextMenuProvider {

	private final ActionRegistry actions;

	ContextMenu(EditPartViewer viewer, ActionRegistry actions) {
		super(viewer);
		this.actions = actions;
	}

	@Override
	public void buildContextMenu(IMenuManager menu) {

		// creation actions
		menu.add(actions.getAction(AddStockAction.ID));
		menu.add(actions.getAction(AddRateAction.ID));
		menu.add(actions.getAction(AddAuxiliaryAction.ID));

		menu.add(new Separator());

		// standard edit actions
		var deleteAction = actions.getAction(ActionFactory.DELETE.getId());
		if (deleteAction != null) {
			menu.add(deleteAction);
		}
	}
}
