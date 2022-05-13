package org.openlca.app.editors.graph.model;

import org.eclipse.draw2d.geometry.Dimension;
import org.openlca.app.M;
import org.openlca.core.model.descriptors.RootDescriptor;

import java.util.ArrayList;

/**
 * A {@link Node} represents a unit process, a library process, a result
 * or a product system with its list of input or output flows (see
 * {@link IOPane}).
 */
public class Node extends MinMaxGraphComponent {

	private static final Dimension DEFAULT_MINIMIZED_SIZE = new Dimension(250, 40);
	private static final Dimension DEFAULT_MAXIMIZED_SIZE = new Dimension(250, 300);

	private final RootDescriptor descriptor;

	public Node(RootDescriptor descriptor) {
		this.descriptor = descriptor;
		setSize(isMinimized() ? DEFAULT_MINIMIZED_SIZE : DEFAULT_MAXIMIZED_SIZE);
	}

	public RootDescriptor getDescriptor() {
		return descriptor;
	}

	@Override
	protected Dimension getMinimizedSize() {
		return DEFAULT_MINIMIZED_SIZE;
	}

	@Override
	protected Dimension getMaximizedSize() {
		return DEFAULT_MAXIMIZED_SIZE;
	}

	public String toString() {
		var prefix = isMinimized() ? M.Minimize : M.Maximize;
		return prefix + descriptor.name;
	}

}
