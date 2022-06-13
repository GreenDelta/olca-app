package org.openlca.app.editors.graphical_legacy;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.openlca.app.components.ModelTransfer;
import org.openlca.app.editors.graphical_legacy.model.ProcessNode;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.RootDescriptor;

class GraphDropListener extends DropTargetAdapter {

	private final GraphEditor editor;

	GraphDropListener(GraphEditor editor) {
		this.editor = editor;
	}

	static void on(GraphEditor editor) {
		var viewer = editor.getGraphicalViewer();
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

		var systemNode = editor.getModel();
		var system = systemNode.getProductSystem();
		var location = editor.getGraphicalViewer()
				.getControl()
				.toControl(e.x, e.y);

		var added = new AtomicBoolean(false);
		ModelTransfer.getDescriptors(e.data)
				.stream()
				.filter(d -> d instanceof RootDescriptor
						&& !system.processes.contains(d.id)
						&& (d.type == ModelType.PROCESS
						|| d.type == ModelType.PRODUCT_SYSTEM
						|| d.type == ModelType.RESULT))
				.map(d -> (RootDescriptor) d)
				.map(d -> {
					system.processes.add(d.id);
					var node = new ProcessNode(editor, d);
					systemNode.add(node);
					return node;
				})
				.forEach(node -> {
					added.set(true);
					node.maximize();
					var rect = new Rectangle(
							location.x,
							location.y,
							Math.max(node.getMinimumWidth(), 250),
							Math.max(node.getMinimumHeight(), 150));
					node.setBox(rect);
				});

		// update the editor
		if (!added.get())
			return;
		editor.setDirty();
		if (editor.getOutline() != null) {
			editor.getOutline().refresh();
		}
	}
}
