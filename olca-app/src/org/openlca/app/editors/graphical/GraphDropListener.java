package org.openlca.app.editors.graphical;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.openlca.app.components.ModelTransfer;
import org.openlca.app.editors.graphical.requests.GraphRequest;
import org.openlca.app.util.Labels;
import org.openlca.app.util.MsgBox;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.core.model.descriptors.RootDescriptor;

import static org.eclipse.gef.RequestConstants.REQ_CREATE;

class GraphDropListener extends DropTargetAdapter {

	private final GraphEditor editor;

	GraphDropListener(GraphEditor editor) {
		this.editor = editor;
	}

	static void on(GraphEditor editor) {
		var viewer = (GraphicalViewer) editor.getAdapter(GraphicalViewer.class);
		var target = new DropTarget(viewer.getControl(),
				DND.DROP_COPY | DND.DROP_MOVE | DND.DROP_DEFAULT);
		target.setTransfer(ModelTransfer.getInstance());
		target.addDropListener(new GraphDropListener(editor));
	}

	@Override
	public void drop(DropTargetEvent e) {
		var transfer = ModelTransfer.getInstance();
		if (!transfer.isSupportedType(e.currentDataType))
			return;

		var graph = editor.getModel();
		var viewer = (GraphicalViewer) editor.getAdapter(GraphicalViewer.class);
		var commandStack = (CommandStack) editor.getAdapter(CommandStack.class);
		var registry = viewer.getEditPartRegistry();

		var graphEditPart = (EditPart) registry.get(graph);
		if (graphEditPart == null)
			return;

		var cursorLocation = new Point(viewer.getControl().toControl(e.x, e.y));

		var added = new AtomicBoolean(false);
		ModelTransfer.getDescriptors(e.data)
			.stream()
			.filter(d -> d instanceof RootDescriptor
				&& !productSystemContains(d)
				&& (d.type == ModelType.PROCESS
				|| d.type == ModelType.PRODUCT_SYSTEM
				|| d.type == ModelType.RESULT))
			.map(d -> (RootDescriptor) d)
			.peek(d -> {
				var request = new GraphRequest(REQ_CREATE);
				request.setDescriptors(d);
				request.setLocation(cursorLocation);
				// Getting the command via GraphXYLayoutEditPolicy and executing it.
				var command = graphEditPart.getCommand(request);
				if (command.canExecute())
					commandStack.execute(command);
				else {
					MsgBox.info("This item cannot be added to the product system `"
						+ Labels.name(d) + "`.");
				}
			})
			.filter(d -> graph.getNode(d.id) != null)
			.forEach(d -> {
				added.set(true);
			});

		// update the editor
		if (!added.get())
			return;
		editor.setDirty();
	}

	private boolean productSystemContains(Descriptor d) {
		if (editor.getProductSystem().processes.contains(d.id)) {
			MsgBox.info("The product system already"
				+ " contains process `"
				+ Labels.name(d) + "`.");
			return true;
		}
		else return false;
	}

}
