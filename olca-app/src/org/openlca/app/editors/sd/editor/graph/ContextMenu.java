package org.openlca.app.editors.sd.editor.graph;

import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.ui.actions.ActionFactory;
import org.openlca.app.M;
import org.openlca.app.editors.sd.editor.graph.actions.SystemAddAction;
import org.openlca.app.editors.sd.editor.graph.actions.SystemEditAction;
import org.openlca.app.editors.sd.editor.graph.actions.VarAddAction;
import org.openlca.app.editors.sd.editor.graph.actions.VarEditAction;
import org.openlca.app.rcp.images.Icon;

class ContextMenu extends ContextMenuProvider {

	private final ActionRegistry actions;

	public ContextMenu(GraphicalViewer viewer, ActionRegistry actions) {
		super(viewer);
		this.actions = actions;
	}

	@Override
	public void buildContextMenu(IMenuManager menu) {

		var addMenu = new MenuManager("Add ...", Icon.ADD.descriptor(), "add-var");
		for (var id : VarAddAction.ids()) {
			var a = actions.getAction(id);
			if (a != null) {
				addMenu.add(a);
			}
		}
		var addSystem = actions.getAction(SystemAddAction.ID);
		if (addSystem != null) {
			addMenu.add(addSystem);
		}
		menu.add(addMenu);

		var edit = actions.getAction(VarEditAction.ID);
		if (edit != null) {
			menu.add(edit);
		}

		var editSystem = actions.getAction(SystemEditAction.ID);
		if (editSystem != null) {
			menu.add(editSystem);
		}

		var delete = actions.getAction(ActionFactory.DELETE.getId());
		if (delete != null && delete.isEnabled()) {
			delete.setText(M.Delete);
			delete.setImageDescriptor(Icon.DELETE.descriptor());
			delete.setDisabledImageDescriptor(Icon.DELETE_DISABLED.descriptor());
			menu.add(delete);
		}
	}
}
