package org.openlca.app.editors.graphical.model.commands;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.app.tools.graphics.model.Component;
import org.openlca.app.tools.graphics.model.commands.ComponentSetConstraintCommand;

/**
 * Command to set the constraints of the children of Graph.
 * Extends ComponentSetConstraintCommand as the editor has to be set dirty.
 */
public class SetConstraintCommand extends ComponentSetConstraintCommand {

	private final GraphEditor editor;

	public SetConstraintCommand(Component component, ChangeBoundsRequest req,
		Rectangle newBounds) {
		super(component, req, newBounds);
		this.editor = ((Graph) component.getParent()).getEditor();
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
