package org.openlca.app.editors.graphical.model.commands;

import java.util.Objects;

import org.eclipse.gef.commands.Command;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.model.ExchangeItem;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Process;
import org.openlca.core.model.descriptors.ProcessDescriptor;

import static org.openlca.app.editors.graphical.model.GraphFactory.updateExchangeItem;

public class EditExchangeCommand extends Command {

	private final ExchangeItem oldExchangeItem;
	private final GraphEditor editor;
	private ExchangeItem newExchangeItem;
	private Process process;
	private Exchange exchange;
	private final Node node;

	public EditExchangeCommand(ExchangeItem exchangeItem) {
		this.oldExchangeItem = exchangeItem;
		this.node = exchangeItem.getNode();
		editor = node.getGraph().getEditor();
	}

	@Override
	public boolean canExecute() {
		if (node == null || node.descriptor == null || !node.isEditable()
				|| !(node.descriptor instanceof ProcessDescriptor))
			return false;

		process = (Process) node.getEntity();
		if (process == null) return false;
		exchange = process.exchanges.stream()
				.filter(e -> Objects.equals(e, oldExchangeItem.exchange))
				.findFirst()
				.orElse(null);

		var node = oldExchangeItem.getNode();
		return node != null
				&& oldExchangeItem.exchange != null
				&& oldExchangeItem.exchange.flow != null
				&& process != null;
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	@Override
	public void execute() {
		if (process == null || exchange == null)
			return;

		if (!ExchangeDialog.open(exchange))
			return;
		newExchangeItem = new ExchangeItem(exchange);
		redo();
	}

	@Override
	public void redo() {
		exchange = newExchangeItem.exchange;
		updateExchangeItem(oldExchangeItem, newExchangeItem);

		editor.setDirty(process);
	}

	@Override
	public void undo() {
		exchange = oldExchangeItem.exchange;
		updateExchangeItem(newExchangeItem, oldExchangeItem);

		editor.setDirty(process);
	}

}
