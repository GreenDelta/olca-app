package org.openlca.app.editors.graphical.model.commands;

import org.eclipse.gef.commands.Command;
import org.openlca.app.db.Database;
import org.openlca.app.editors.graphical.model.ExchangeItem;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Process;
import org.openlca.core.model.descriptors.Descriptor;

import java.util.Objects;

import static org.openlca.app.editors.graphical.model.GraphFactory.updateExchangeItem;

public class SetReferenceCommand extends Command {

	private final IDatabase db = Database.get();
	private final Node node;
	private final ExchangeItem child;
	private final ExchangeItem oldRefExchangeItem;
	private Exchange oldRefExchange;

	public SetReferenceCommand(ExchangeItem child) {
		this.child = child;
		this.node = child.getNode();
		var process = db.get(Process.class, node.descriptor.id);
		this.oldRefExchangeItem = node.getRefExchangeItem();
		if (oldRefExchangeItem != null)
			this.oldRefExchange = getExchange(process, oldRefExchangeItem);
	}

	@Override
	public boolean canExecute() {
		return child.canBeReferenceFlow()
			&& !child.isQuantitativeReference()
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
		redo();
	}

	@Override
	public void redo() {
		if (child == null)
			return;

		update(child, oldRefExchange);
		child.getGraph().getEditor().setDirty();
	}

	@Override
	public void undo() {
		if (child == null)
			return;

		var process = db.get(Process.class, node.descriptor.id);
		// Getting the old reference Exchange.
		var oldRefExchangeItem = node.getExchangeItem(oldRefExchange);
		var oldExchange = getExchange(process, oldRefExchangeItem);
		update(oldRefExchangeItem, oldExchange);

		child.getGraph().getEditor().setDirty();
	}

	private void update(ExchangeItem newRefExchangeItem, Exchange oldRefExchange) {
		var process = db.get(Process.class, node.descriptor.id);
		var newRefExchange = getExchange(process, newRefExchangeItem);
		if (newRefExchange == null)
			return;
		process.quantitativeReference = newRefExchange;
		db.update(process);

		// Updating the new and the old quantitative reference.
		var descriptor = Descriptor.of(process);

		if (oldRefExchange != null)
			updateExchangeItem(node, descriptor, oldRefExchangeItem,
				new ExchangeItem(oldRefExchange));

		updateExchangeItem(node, descriptor, newRefExchangeItem,
			new ExchangeItem(newRefExchange));
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
