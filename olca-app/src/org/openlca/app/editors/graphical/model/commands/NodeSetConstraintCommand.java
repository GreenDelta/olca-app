package org.openlca.app.editors.graphical.model.commands;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.app.tools.graphics.model.Component;
import org.openlca.app.tools.graphics.model.commands.ComponentSetConstraintCommand;

public class NodeSetConstraintCommand extends ComponentSetConstraintCommand {

	private final GraphEditor editor;

	public NodeSetConstraintCommand(Component component, ChangeBoundsRequest req, Rectangle newBounds) {
		super(component, req, newBounds);
		this.editor = ((Node) component).getGraph().getEditor();
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
