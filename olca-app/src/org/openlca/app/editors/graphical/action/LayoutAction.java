package org.openlca.app.editors.graphical.action;

import org.eclipse.jface.action.Action;
import org.eclipse.osgi.util.NLS;
import org.openlca.app.Messages;
import org.openlca.app.editors.graphical.command.CommandFactory;
import org.openlca.app.editors.graphical.command.LayoutCommand;
import org.openlca.app.editors.graphical.layout.GraphLayoutManager;
import org.openlca.app.editors.graphical.layout.GraphLayoutType;
import org.openlca.app.editors.graphical.model.ProductSystemNode;

class LayoutAction extends Action {

	private ProductSystemNode model;
	private GraphLayoutType layoutType;

	LayoutAction(GraphLayoutType layoutType) {
		setText(NLS.bind(Messages.LayoutAs,
				layoutType.getDisplayName()));
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
		LayoutCommand command = CommandFactory.createLayoutCommand(model,
				(GraphLayoutManager) model.getFigure().getLayoutManager(),
				layoutType);
		model.getEditor().getCommandStack().execute(command);
	}

	void setModel(ProductSystemNode model) {
		this.model = model;
	}

}
