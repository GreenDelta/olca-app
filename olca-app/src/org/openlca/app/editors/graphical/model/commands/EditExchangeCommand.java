package org.openlca.app.editors.graphical.model.commands;

import org.eclipse.gef.commands.Command;
import org.openlca.app.db.Database;
import org.openlca.app.editors.graphical.model.ExchangeItem;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Process;

import java.util.Objects;

public class EditExchangeCommand extends Command {

	private final ExchangeItem oldExchangeItem;
	private ExchangeItem newExchangeItem;
	private final IDatabase db = Database.get();
	private final Process process;
	private final Node node;

	public EditExchangeCommand(ExchangeItem exchangeItem) {
		this.oldExchangeItem = exchangeItem;
		this.node = exchangeItem.getNode();
		process = db.get(Process.class, node.descriptor.id);
	}

	@Override
	public boolean canExecute() {
		var node = oldExchangeItem.getNode();
		return oldExchangeItem.exchange != null
			&& oldExchangeItem.exchange.flow != null
			&& node != null
			&& node.descriptor != null
			&& node.isEditable();
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	@Override
	public void execute() {
		if (process == null)
			return;
		var exchange = process.exchanges.stream()
			.filter(e -> Objects.equals(e, oldExchangeItem.exchange))
			.findFirst()
			.orElse(null);
		if (exchange == null)
			return;
		if (!EditExchangeDialog.open(exchange))
			return;
		db.update(process);

		newExchangeItem = new ExchangeItem(node.getGraph().editor, exchange);
		redo();
	}

	@Override
	public void redo() {
		updateExchangeItem(oldExchangeItem, newExchangeItem);
	}

	@Override
	public void undo() {
		updateExchangeItem(newExchangeItem, oldExchangeItem);
	}

	private void updateExchangeItem(ExchangeItem oldValue, ExchangeItem newValue) {
		var forInput = oldValue.getIOPane().isForInputs();
		var ioPane = forInput ? node.getInputIOPane() : node.getOutputIOPane();

		var sourceLink = oldValue.getSourceConnections();
		var targetLink = oldValue.getTargetConnections();

		// Disconnecting the links before removing pane's child.
		for (var link : oldValue.getAllLinks())
			link.disconnect();

		ioPane.removeChild(oldValue);
		ioPane.addChild(newValue);

		for (var link : sourceLink)
			link.reconnect(newValue, link.getTarget());
		for (var link : targetLink)
			link.reconnect(link.getSource(), newValue);
	}

}
