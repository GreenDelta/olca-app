package org.openlca.app.editors.graph.model.commands;

import org.eclipse.gef.commands.Command;
import org.openlca.app.db.Database;
import org.openlca.app.editors.graph.model.ExchangeItem;
import org.openlca.app.editors.graph.model.Link;
import org.openlca.app.editors.graph.model.Node;
import org.openlca.app.editors.graphical.action.GraphActions;
import org.openlca.app.editors.graphical.model.ExchangeNode;
import org.openlca.app.editors.graphical.model.IONode;
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

import static org.openlca.app.editors.graph.model.GraphComponent.INPUT_PROP;
import static org.openlca.app.editors.graph.model.GraphComponent.OUTPUT_PROP;

public class EditExchangeCommand extends Command {

	private final ExchangeItem exchangeItem;
	private final IDatabase db = Database.get();
	private final Process process;
	private final Node node;

	public EditExchangeCommand(ExchangeItem exchangeItem) {
		this.exchangeItem = exchangeItem;
		this.node = exchangeItem.getNode();
		process = db.get(Process.class, node.descriptor.id);
	}

	@Override
	public boolean canExecute() {
		var node = exchangeItem.getNode();
		return exchangeItem.exchange != null
			&& exchangeItem.exchange.flow != null
			&& node != null
			&& node.descriptor != null
			&& node.isEditable();
	}

	@Override
	public boolean canUndo() {
		// TODO (francois) Implement undo.
		return false;
	}

	@Override
	public void execute() {
		redo();
	}

	@Override
	public void redo() {
		if (process == null)
			return;
		var exchange = process.exchanges.stream()
			.filter(e -> Objects.equals(e, exchangeItem.exchange))
			.findFirst()
			.orElse(null);
		if (exchange == null)
			return;
		if (!EditExchangeDialog.open(exchange))
			return;
		db.update(process);

		var forInput = exchangeItem.getIOPane().isForInputs();
		var ioPane = forInput ? node.getInputIOPane() : node.getOutputIOPane();
		node.removeChild(ioPane);
		var panes = node.editor.getGraphFactory().createIOPanes(node.descriptor);
		node.addChild(
			panes.get(forInput ? INPUT_PROP : OUTPUT_PROP), forInput ? 0 : 1);
	}

}
