package org.openlca.app.editors.graph.model.commands;

import java.util.List;

import org.eclipse.gef.commands.Command;
import org.openlca.app.editors.graph.model.Graph;
import org.openlca.app.editors.graph.model.Link;
import org.openlca.app.editors.graph.model.Node;

public class NodeDeleteCommand extends Command {
	/** Node to remove. */
	private final Node child;
	/** Graph to remove from. */
	private final Graph parent;

	/** Holds a copy of the outgoing connections of child. */
	private List<Link> sourceConnections;
	/** Holds a copy of the incoming connections of child. */
	private List<Link> targetConnections;
	/** True, if child was removed from its parent. */
	private boolean wasRemoved;

	/**
	 * Create a command that will remove the node from its parent.
	 *
	 * @param parent
	 *            the parent containing the child
	 * @param child
	 *            the component to remove
	 * @throws IllegalArgumentException
	 *             if any parameter is null
	 */
	public NodeDeleteCommand(Graph parent, Node child) {
		if (parent == null || child == null) {
			throw new IllegalArgumentException();
		}
		setLabel("delete node");
		this.parent = parent;
		this.child = child;
	}

	@Override
	public boolean canExecute() {
		if (child == null)
			return false;
		long refID = child.getGraph().getProductSystem().referenceProcess.id;
		return child.descriptor.id != refID;
	}

	@Override
	public boolean canUndo() {
		return wasRemoved;
	}

	@Override
	public void execute() {
		// store a copy of incoming & outgoing links before proceeding
		sourceConnections = child.getSourceConnections();
		targetConnections = child.getTargetConnections();
		redo();
	}

	@Override
	public void redo() {
		// remove the child and disconnect its links
		parent.getProductSystem().processes.remove(child.descriptor.id);
		wasRemoved = parent.removeChild(child);
		if (wasRemoved) {
			removeConnections(sourceConnections);
			removeConnections(targetConnections);
		}
		parent.editor.setDirty();
	}


	/**
	 * Reconnects a List of Links with their previous endpoints.
	 *
	 * @param links
	 *            a non-null List of links
	 */
	private void addConnections(List<Link> links) {
		for (Link link : links) {
			link.reconnect();
		}
	}

	private void removeConnections(List<Link> links) {
		for (Link link : links) {
			link.disconnect();
		}
	}

	@Override
	public void undo() {
		// add the child and reconnect its links
		parent.getProductSystem().processes.add(child.descriptor.id);
		parent.addChild(child);
		addConnections(sourceConnections);
		addConnections(targetConnections);
	}

}
