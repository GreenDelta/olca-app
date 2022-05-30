package org.openlca.app.editors.graph;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.openlca.app.components.ModelTransfer;
import org.openlca.app.editors.graph.layouts.NodeLayoutInfo;
import org.openlca.app.editors.graph.model.Node;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.RootDescriptor;

import java.util.concurrent.atomic.AtomicBoolean;

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
		System.out.println("In drop");
		var transfer = ModelTransfer.getInstance();
		if (!transfer.isSupportedType(e.currentDataType))
			return;

		var graph = editor.getModel();
		var productSystem = editor.getProductSystem();
		var graphFactory = editor.getGraphFactory();
		var location = editor.getGraphicalViewer()
				.getControl()
				.toControl(e.x, e.y);

		var added = new AtomicBoolean(false);
		ModelTransfer.getDescriptors(e.data)
			.stream()
			.peek(d -> System.out.println("!productSystem.processes.contains(d.id)" + !productSystem.processes.contains(d.id)))
			.filter(d -> d instanceof RootDescriptor
				&& !productSystem.processes.contains(d.id)
				&& (d.type == ModelType.PROCESS
				|| d.type == ModelType.PRODUCT_SYSTEM
				|| d.type == ModelType.RESULT))
			.map(d -> {
				System.out.println("Her1e");
				return (RootDescriptor) d;
			})
			.map(d -> {
				System.out.println("Here");
					productSystem.processes.add(d.id);
					var info = new NodeLayoutInfo(new Point(location.x, location.y),
						null, true, false, false);
					var node = graphFactory.createNode(d, info);
					System.out.println("Add node" + node);
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
