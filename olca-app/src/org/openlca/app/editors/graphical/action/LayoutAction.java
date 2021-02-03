package org.openlca.app.editors.graphical.action;

import org.eclipse.jface.action.Action;
import org.eclipse.osgi.util.NLS;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.command.LayoutCommand;
import org.openlca.app.editors.graphical.layout.LayoutManager;
import org.openlca.app.editors.graphical.layout.LayoutType;
import org.openlca.app.editors.graphical.model.ProductSystemNode;

class LayoutAction extends Action {

	private ProductSystemNode model;
	private final LayoutType layoutType;

	LayoutAction(LayoutType layoutType) {
		setText(NLS.bind(M.LayoutAs, layoutType.getDisplayName()));
		switch (layoutType) {
		case TREE_LAYOUT:
			setId(ActionIds.LAYOUT_TREE);
			break;
		case MINIMAL_TREE_LAYOUT:
			setId(ActionIds.LAYOUT_MINIMAL_TREE);
			break;
		}
		this.layoutType = layoutType;
	}

	@Override
	public void run() {
		LayoutManager layoutManager = (LayoutManager) model.figure.getLayoutManager();
		LayoutCommand command = new LayoutCommand(model, layoutManager, layoutType);
		model.editor.getCommandStack().execute(command);
	}

	void setModel(ProductSystemNode model) {
		this.model = model;
	}

}
