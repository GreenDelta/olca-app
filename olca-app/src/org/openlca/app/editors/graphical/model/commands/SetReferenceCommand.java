package org.openlca.app.editors.graphical.model.commands;

import org.eclipse.gef.commands.Command;
import org.openlca.app.db.Database;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.model.ExchangeItem;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;

import java.util.Objects;

import static org.openlca.app.editors.graphical.model.GraphFactory.updateExchangeItem;

public class SetReferenceCommand extends Command {

	private final IDatabase db = Database.get();
	private final Node node;
	private final ExchangeItem child;
	private ExchangeItem oldRefExchangeItem;
	private final GraphEditor editor;
	private Exchange oldRefExchange;
	private Process process;

	public SetReferenceCommand(ExchangeItem child) {
		this.child = child;
		node = child.getNode();
		editor = node.getGraph().getEditor();
	}

	@Override
	public boolean canExecute() {
		if (!child.canBeReferenceFlow()
			|| child.isQuantitativeReference()
			|| node == null
			|| node.descriptor == null
			|| !node.isEditable()
		  || node.descriptor.type != ModelType.PROCESS)
			return false;

		process = (Process) node.getEntity();
		this.oldRefExchangeItem = node.getRefExchangeItem();
		if (oldRefExchangeItem != null)
			this.oldRefExchange = getExchange(process, oldRefExchangeItem);

		return process != null;
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	@Override
	public void execute() {
		redo();
	}

	@Override
	public void redo() {
		if (child == null)
			return;

		update(child, oldRefExchange);

		editor.setDirty(process);
	}

	@Override
	public void undo() {
		if (child == null)
			return;

		// Getting the old reference Exchange.
		var oldRefExchangeItem = node.getExchangeItem(oldRefExchange);
		var oldExchange = getExchange(process, oldRefExchangeItem);
		update(oldRefExchangeItem, oldExchange);

		editor.setDirty(process);
	}

	private void update(ExchangeItem newRefExchangeItem, Exchange oldRefExchange) {
		var newRefExchange = newRefExchangeItem.exchange;
		if (newRefExchange == null)
			return;
		process.quantitativeReference = newRefExchange;

		if (oldRefExchange != null)
			updateExchangeItem(oldRefExchangeItem, new ExchangeItem(oldRefExchange));

		updateExchangeItem(newRefExchangeItem, new ExchangeItem(newRefExchange));
	}

	private Exchange getExchange(Process process, ExchangeItem item) {
		if (process == null)
			return null;
		return process.exchanges.stream()
			.filter(e -> Objects.equals(e.id, item.exchange.id))
			.findFirst()
			.orElse(null);
	}

}
