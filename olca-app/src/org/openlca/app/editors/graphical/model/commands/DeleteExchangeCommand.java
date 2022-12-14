package org.openlca.app.editors.graphical.model.commands;

import org.eclipse.gef.commands.Command;
import org.eclipse.osgi.util.NLS;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.model.*;
import org.openlca.app.util.Labels;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Question;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.usage.ExchangeUseSearch;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;

import java.util.ArrayList;
import java.util.Objects;

import static org.openlca.app.editors.processes.exchanges.Exchanges.canRemove;

public class DeleteExchangeCommand extends Command {

	/**
	 * ExchangeItem to remove.
	 */
	private final ExchangeItem child;
	/**
	 * IOPane to remove from.
	 */
	private final IOPane parent;
	/**
	 * Node to remove from.
	 */
	private final Node node;
	private final Graph graph;
	private final GraphEditor editor;


	private final IDatabase db = Database.get();
	private Process process;
	private Exchange exchange;

	/**
	 * Create a command that will remove the exchange item from its parent.
	 *
	 * @param parent the parent containing the child
	 * @param child  the component to remove
	 * @throws IllegalArgumentException if any parameter is null
	 */
	public DeleteExchangeCommand(IOPane parent, ExchangeItem child) {
		if (parent == null || child == null) {
			throw new IllegalArgumentException();
		}
		setLabel(NLS.bind(M.Delete.toLowerCase(), M.Flow));
		this.parent = parent;
		this.child = child;
		node = child.getNode();
		graph = node.getGraph();
		editor = graph.getEditor();
	}

	@Override
	public boolean canExecute() {
		if (node == null || node.descriptor == null
				|| node.descriptor.type != ModelType.PROCESS
				|| child.exchange == null
				|| node.descriptor.isFromLibrary())
			return false;

		process = (Process) node.getEntity();
		exchange = getExchange();

		return process != null && exchange != null;
	}

	@Override
	public void execute() {
		redo();
	}

	@Override
	public void redo() {
		// remove the child and disconnect its links
		if (exchange == null)
			return;

		var exchanges = new ArrayList<Exchange>();
		exchanges.add(exchange);
		if (!canRemove(process, exchanges))
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


		process.exchanges.remove(exchange);
		var processLinks = graph.linkSearch.getLinks(process.id);
		for (var link : processLinks) {
			if (link.exchangeId == exchange.id) {
				system.processLinks.remove(link);
				var graphLink = graph.mapProcessLinkToGraphLink.remove(link);
				if (graphLink != null)
					graphLink.disconnect();
			}
		}

		parent.removeChild(child);

		editor.setDirty(process);
	}

	private Exchange getExchange() {
		if (process == null)
			return null;
		return process.exchanges.stream()
				.filter(e -> Objects.equals(e, child.exchange))
				.findFirst()
				.orElse(null);
	}

}
