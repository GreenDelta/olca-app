package org.openlca.app.editors.graphical.model.commands;

import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.tools.graphics.model.Component;
import org.openlca.app.tools.graphics.model.commands.LayoutCommand;

public class GraphLayoutCommand extends LayoutCommand {

	private final GraphEditor editor;

	public GraphLayoutCommand(Component parent) {
		super(parent);
		this.editor = ((Graph) parent).editor;
	}


	@Override
	public void redo() {
		super.redo();
		editor.setDirty();
	}

	@Override
	public void undo() {
		super.undo();
		editor.setDirty();
	}

}
