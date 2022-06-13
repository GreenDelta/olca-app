package org.openlca.app.editors.graphical;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.openlca.app.components.ModelTransfer;
import org.openlca.app.editors.graphical.layouts.NodeLayoutInfo;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.RootDescriptor;

import java.util.concurrent.atomic.AtomicBoolean;

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
		var productSystem = editor.getProductSystem();
		var graphFactory = editor.getGraphFactory();
		var viewer = (GraphicalViewer) editor.getAdapter(GraphicalViewer.class);
		var location = viewer.getControl().toControl(e.x, e.y);

		var added = new AtomicBoolean(false);
		ModelTransfer.getDescriptors(e.data)
			.stream()
			.filter(d -> d instanceof RootDescriptor
				&& !productSystem.processes.contains(d.id)
				&& (d.type == ModelType.PROCESS
				|| d.type == ModelType.PRODUCT_SYSTEM
				|| d.type == ModelType.RESULT))
			.map(d -> (RootDescriptor) d)
			.map(d -> {
					productSystem.processes.add(d.id);
					var info = new NodeLayoutInfo(new Point(location.x, location.y),
						null, true, false, false);
					var node = graphFactory.createNode(d, info);
					graph.addChild(node);
					return node;
				})
				.forEach(node -> {
					added.set(true);
				});

		// update the editor
		if (!added.get())
			return;
		editor.setDirty();
	}
}
