package org.openlca.app.editors.graphical.model.commands;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.eclipse.osgi.util.NLS;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.layouts.NodeLayoutInfo;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.app.util.Labels;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.RootDescriptor;

public class CreateNodeCommand extends Command {

	private final GraphEditor editor;
	private final Graph graph;
	private final RootDescriptor descriptor;
	private final Rectangle constraint;
	private int index = -1;
	private Node node;

	public CreateNodeCommand(Graph graph, RootDescriptor descriptor,
													 Rectangle constraint) {
		this.graph = graph;
		this.descriptor = descriptor;
		this.constraint = constraint;
		this.editor = graph.editor;
		setLabel(NLS.bind(M.Add.toLowerCase(), Labels.name(descriptor)));
	}

	public CreateNodeCommand(Graph graph, RootDescriptor descriptor,
													 Rectangle constraint, int index) {
		this(graph, descriptor, constraint);
		this.index = index;
	}

	public void execute() {
		if (descriptor.type != ModelType.PROCESS
			&& descriptor.type != ModelType.PRODUCT_SYSTEM) {
			return;
		}

		// Add the process to the product system.
		var system = graph.getProductSystem();
		system.processes.add(descriptor.id);

		// Add the process to the graph.
		var location = constraint.getLocation();
		var size = (new Dimension(-1, -1)).equals(constraint.getSize())
			? Node.DEFAULT_SIZE
			: constraint.getSize();
		var info = new NodeLayoutInfo(location, size, false, false, false);
		node = graph.editor.getGraphFactory().createNode(descriptor, info);
		if (this.index > 0)
			this.graph.addChild(node, this.index);
		else this.graph.addChild(node);

		editor.setDirty();
	}

	public void undo() {
		// Remove the process from the product system.
		var system = graph.getProductSystem();
		system.processes.remove(descriptor.id);
		// Remove the node from the graph's children.
		this.graph.removeChild(node);
	}

}
