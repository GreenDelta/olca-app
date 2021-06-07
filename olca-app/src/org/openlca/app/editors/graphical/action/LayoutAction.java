package org.openlca.app.editors.graphical.action;

import org.eclipse.jface.action.Action;
import org.eclipse.osgi.util.NLS;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.command.LayoutCommand;
import org.openlca.app.editors.graphical.layout.LayoutType;

class LayoutAction extends Action {

	private final GraphEditor editor;
	private final LayoutType layoutType;

	LayoutAction(GraphEditor editor, LayoutType layoutType) {
		this.editor = editor;
		setText(NLS.bind(M.LayoutAs, layoutType.getDisplayName()));
		switch (layoutType) {
			case TREE_LAYOUT -> setId(ActionIds.LAYOUT_TREE);
			case MINIMAL_TREE_LAYOUT -> setId(ActionIds.LAYOUT_MINIMAL_TREE);
		}
		this.layoutType = layoutType;
	}

	@Override
	public void run() {
		var command = new LayoutCommand(editor, layoutType);
		editor.getCommandStack().execute(command);
	}
}
