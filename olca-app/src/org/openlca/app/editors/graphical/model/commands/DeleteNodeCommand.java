package org.openlca.app.editors.graphical.model.commands;

import java.util.List;

import org.eclipse.gef.commands.Command;
import org.openlca.app.db.Database;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.GraphLink;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.app.util.Labels;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Question;
import org.openlca.core.database.usage.ExchangeUseSearch;
import org.openlca.core.database.usage.ProcessUseSearch;

public class DeleteNodeCommand extends Command {
	/** Node to remove. */
	private final Node child;
	/** Graph to remove from. */
	private final Graph parent;

	/** Holds a copy of all the links of the child and sub-child. */
	private List<GraphLink> links;
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
	public DeleteNodeCommand(Graph parent, Node child) {
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
		var b = Question.ask("Remove process",
				"Remove " + Labels.name(child.descriptor)
						+ " from product system " + Labels.name(parent.getProductSystem())
						+ "?");
		if (!b)
			return;

		// store a copy of incoming & outgoing links before proceeding
		links = child.getAllLinks().stream().map(GraphLink.class::cast).toList();
		redo();
	}

	@Override
	public void redo() {
		// remove the child and disconnect its links
		parent.getProductSystem().processes.remove(child.descriptor.id);
		wasRemoved = parent.removeChild(child);
		if (wasRemoved) {
			removeConnections(links);
		}
		parent.editor.setDirty();
	}


	/**
	 * Reconnects a List of Links with their previous endpoints.
	 *
	 * @param links
	 *            a non-null List of links
	 */
	private void addConnections(List<GraphLink> links) {
		for (GraphLink link : links) {
			parent.linkSearch.put(link.processLink);
			parent.getProductSystem().processLinks.add(link.processLink);
			parent.mapProcessLinkToGraphLink.put(link.processLink, link);
			link.reconnect();
		}
	}

	private void removeConnections(List<GraphLink> links) {
		for (GraphLink link : links) {
			parent.removeLink(link.processLink);
		}
	}

	@Override
	public void undo() {
		// add the child and reconnect its links
		parent.getProductSystem().processes.add(child.descriptor.id);
		parent.addChild(child);
		addConnections(links);

		parent.editor.setDirty();
	}

}
