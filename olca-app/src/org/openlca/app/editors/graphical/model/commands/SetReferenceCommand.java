package org.openlca.app.editors.graphical.model.commands;

import org.eclipse.gef.commands.Command;
import org.openlca.app.db.Database;
import org.openlca.app.editors.graphical.model.ExchangeItem;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Process;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.core.model.descriptors.RootDescriptor;

import java.util.Objects;

import static org.openlca.app.editors.graphical.model.GraphFactory.updateExchangeItem;

public class SetReferenceCommand extends Command {

	private final IDatabase db = Database.get();
	private final Node node;
	private final ExchangeItem child;
	private Exchange oldRefExchange;

	public SetReferenceCommand(ExchangeItem child) {
		this.child = child;
		this.node = child.getNode();
		var process = db.get(Process.class, node.descriptor.id);
		var currentRefExchangeItem = node.getRefExchangeItem();
		if (currentRefExchangeItem != null)
			this.oldRefExchange = getExchange(process, currentRefExchangeItem);
	}

	@Override
	public boolean canExecute() {
		return child.canBeReferenceFlow()
			&& !child.isRefFlow()
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
		child.editor.setDirty();
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

		child.editor.setDirty();
	}

	private void update(ExchangeItem newRefExchangeItem, Exchange oldRefExchange) {
		var process = db.get(Process.class, node.descriptor.id);
		var newRefExchange = getExchange(process, newRefExchangeItem);
		process.quantitativeReference = newRefExchange;
		db.update(process);

		// Updating the new and the old quantitative reference.
		var descriptor = Descriptor.of(process);

		if (oldRefExchange != null)
			updateExchangeItem(node, descriptor, node.getRefExchangeItem(),
				new ExchangeItem(node.editor, oldRefExchange));

		updateExchangeItem(node, descriptor, newRefExchangeItem,
			new ExchangeItem(node.editor, newRefExchange));
	}

	private Exchange getExchange(Process process, ExchangeItem item) {
		if (process == null)
			return null;
		return process.exchanges.stream()
			.filter(e -> Objects.equals(e, item.exchange))
			.findFirst()
			.orElse(null);
	}

}
