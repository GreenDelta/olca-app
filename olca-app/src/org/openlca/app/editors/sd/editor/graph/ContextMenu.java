package org.openlca.app.editors.sd.editor.graph;

import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.actions.ActionFactory;
import org.openlca.app.M;
import org.openlca.app.editors.sd.editor.graph.actions.AddRateAction;
import org.openlca.app.rcp.images.Icon;

class ContextMenu extends ContextMenuProvider {

	private final ActionRegistry actions;

	public ContextMenu(GraphicalViewer viewer, ActionRegistry actions) {
		super(viewer);
		this.actions = actions;
	}

	@Override
	public void buildContextMenu(IMenuManager menu) {
		menu.add(actions.getAction(AddRateAction.ID));
		var delete = actions.getAction(ActionFactory.DELETE.getId());
		if (delete != null && delete.isEnabled()) {
			delete.setText(M.Delete);
			delete.setImageDescriptor(Icon.DELETE.descriptor());
			delete.setDisabledImageDescriptor(Icon.DELETE_DISABLED.descriptor());
			menu.add(delete);
		}
	}
}
