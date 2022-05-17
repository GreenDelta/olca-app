package org.openlca.app.editors.graph.model;

import org.eclipse.draw2d.geometry.Dimension;
import org.openlca.app.M;
import org.openlca.app.editors.graph.layouts.NodeLayoutInfo;
import org.openlca.app.editors.graph.GraphEditor;
import org.openlca.core.model.descriptors.RootDescriptor;

/**
 * A {@link Node} represents a unit process, a library process, a result
 * or a product system with its list of input or output flows (see
 * {@link IOPane}).
 */
public class Node extends MinMaxGraphComponent {

	private static final Dimension DEFAULT_MINIMIZED_SIZE = new Dimension(250, 40);
	private static final Dimension DEFAULT_MAXIMIZED_SIZE = new Dimension(250, 300);

	public final RootDescriptor descriptor;

	public Node(RootDescriptor descriptor, GraphEditor editor) {
		super(editor);
		this.descriptor = descriptor;
		setSize(isMinimized() ? DEFAULT_MINIMIZED_SIZE : DEFAULT_MAXIMIZED_SIZE);
	}

	public void apply(NodeLayoutInfo info) {
		setSize(info.box.getSize());
		setLocation(info.box.getLocation());

		// TODO Expanders
	}

	@Override
	protected Dimension getMinimizedSize() {
		return DEFAULT_MINIMIZED_SIZE;
	}

	@Override
	protected Dimension getMaximizedSize() {
		return DEFAULT_MAXIMIZED_SIZE;
	}

	@Override
	public void addChildren() {
		if (descriptor == null || descriptor.type == null)
			return;
		var panes = editor.getGraphFactory().createIOPanes(descriptor);
		addChild(panes.get("input"), 0);
		addChild(panes.get("output"), 1);
	}

	public String toString() {
		var prefix = isMinimized() ? M.Minimize : M.Maximize;
		return prefix + descriptor.name;
	}
}
