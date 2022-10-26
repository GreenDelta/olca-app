package org.openlca.app.editors.graphical.actions;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.eclipse.swt.widgets.Display;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.requests.GraphRequest;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.MsgBox;

import static org.eclipse.gef.RequestConstants.REQ_CREATE;

public class AddStickyNoteAction extends WorkbenchPartAction {

	private Graph graph;
	private final GraphEditor editor;
	private org.eclipse.swt.graphics.Point cursorLocation;

	public AddStickyNoteAction(GraphEditor part) {
		super(part);
		editor = part;
		setId(GraphActionIds.ADD_STICKY_NOTE);
		setText(M.AddStickyNote);
		setImageDescriptor(Icon.COMMENT.descriptor());
	}

	@Override
	public void run() {
		var viewer = (GraphicalViewer) getWorkbenchPart().getAdapter(
				GraphicalViewer.class);
		var registry = viewer.getEditPartRegistry();
		var graphEditPart = (EditPart) registry.get(graph);
		if (graphEditPart == null)
			return;

		var cursorLocationInViewport = new Point(viewer.getControl()
				.toControl(cursorLocation));
		var request = new GraphRequest(REQ_CREATE);
		request.setLocation(cursorLocationInViewport);
		// Getting the command via GraphXYLayoutEditPolicy and executing it.
		var command = graphEditPart.getCommand(request);
		if (command.canExecute())
			execute(command);
		else {
			MsgBox.info("No sticky note can be added to the graph.");
		}
	}

	@Override
	protected boolean calculateEnabled() {
		cursorLocation = Display.getCurrent().getCursorLocation();
		if (editor != null) {
			graph = editor.getModel();
			return editor.getProductSystem() != null;
		}
		else return false;
	}

}
