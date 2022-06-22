package org.openlca.app.editors.graphical.model.commands;

import org.eclipse.gef.commands.Command;
import org.eclipse.osgi.util.NLS;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.graphical.model.*;
import org.openlca.app.util.Labels;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Question;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.usage.ExchangeUseSearch;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;

import java.util.List;
import java.util.Objects;

public class DeleteExchangeCommand extends Command {

	/** ExchangeItem to remove. */
	private final ExchangeItem child;
	/** IOPane to remove from. */
	private final IOPane parent;
	/** Node to remove from. */
	private final Node node;


	/** Holds a copy of all the links of the child and sub-child. */
	private List<Link> links;
	/** True, if child was removed from its parent. */
	private boolean wasRemoved;
	private final IDatabase db = Database.get();
	private final Process process;
	private final Exchange exchange;

	/**
	 * Create a command that will remove the exchange item from its parent.
	 *
	 * @param parent
	 *            the parent containing the child
	 * @param child
	 *            the component to remove
	 * @throws IllegalArgumentException
	 *             if any parameter is null
	 */
	public DeleteExchangeCommand(IOPane parent, ExchangeItem child) {
		if (parent == null || child == null) {
			throw new IllegalArgumentException();
		}
		setLabel(NLS.bind(M.Delete.toLowerCase(), M.Flow));
		this.parent = parent;
		this.child = child;
		this.node = child.getNode();
		this.process = db.get(Process.class, node.descriptor.id);
		this.exchange = getExchange();
	}

	@Override
	public boolean canExecute() {
		return child.exchange != null
			&& child.exchange.flow != null
			&& node != null
			&& node.descriptor != null
			&& !node.descriptor.isFromLibrary()
			&& node.descriptor.type == ModelType.PROCESS;
	}

	@Override
	public boolean canUndo() {
		return wasRemoved;
	}

	@Override
	public void execute() {
		// store a copy of incoming & outgoing links before proceeding
		links = child.getAllLinks();
		redo();
	}

	@Override
	public void redo() {
		// remove the child and disconnect its links
		if (exchange == null)
			return;

		// check that the exchange is not used in other models
		var system = child.getGraph().getProductSystem();
		var usages = new ExchangeUseSearch(db, process)
			.findUses(exchange);
		for (var d : usages) {
			if (d.id == system.id || d.id == process.id)
				continue;
			MsgBox.error("Used in other models",
				Labels.name(exchange.flow)
					+ " is used in other models "
					+ "and cannot be deleted");
			return;
		}

		var b = Question.ask("Remove exchange",
			"Remove flow " + Labels.name(exchange.flow)
				+ " from process " + Labels.name(process) + "?");
		if (!b)
			return;

		// TODO: we need to remove process links of that exchange

		process.exchanges.remove(exchange);
		db.update(process);

		wasRemoved = parent.removeChild(child);
		if (wasRemoved) {
			removeConnections(links);
		}

		child.editor.setDirty();
	}

	private Exchange getExchange() {
		if (process == null)
			return null;
		return process.exchanges.stream()
			.filter(e -> Objects.equals(e, child.exchange))
			.findFirst()
			.orElse(null);
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
		process.add(exchange);
		parent.addChild(child);

		addConnections(links);
	}

}
