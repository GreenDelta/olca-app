package org.openlca.app.editors.graphical_legacy.action;

import java.util.Objects;

import org.eclipse.jface.action.Action;
import org.openlca.app.db.Database;
import org.openlca.app.editors.graphical_legacy.GraphEditor;
import org.openlca.app.editors.graphical_legacy.model.ExchangeNode;
import org.openlca.app.editors.graphical_legacy.model.IONode;
import org.openlca.app.editors.graphical_legacy.model.ProcessNode;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Labels;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Question;
import org.openlca.core.database.usage.ExchangeUseSearch;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;

/**
 * Delete a flow from a process.
 */
public class ExchangeDeleteAction extends Action implements GraphAction {

	private ExchangeNode exchangeNode;
	private ProcessNode processNode;

	public ExchangeDeleteAction() {
		setId("ExchangeDeleteAction");
		setImageDescriptor(Icon.DELETE.descriptor());
		setText("Delete exchange");
	}

	@Override
	public boolean accepts(GraphEditor editor) {
		var nodes = GraphActions.allSelectedOf(editor, ExchangeNode.class);
		if (nodes.size() != 1)
			return false;
		var exchangeNode = nodes.get(0);
		var processNode = exchangeNode.parent();
		if (exchangeNode.exchange == null
				|| exchangeNode.exchange.flow == null
				|| processNode == null
				|| processNode.process == null
				|| processNode.process.isFromLibrary()
				|| processNode.process.type != ModelType.PROCESS)
			return false;
		this.processNode = processNode;
		this.exchangeNode = exchangeNode;
		return true;
	}

	@Override
	public void run() {
		var db = Database.get();
		var process = db.get(Process.class, processNode.process.id);
		if (process == null)
			return;
		var exchange = process.exchanges.stream()
				.filter(e -> Objects.equals(e, exchangeNode.exchange))
				.findFirst()
				.orElse(null);
		if (exchange == null)
			return;

		// check that the exchange is not used in other models
		var system = processNode.parent().getProductSystem();
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
		processNode.getChildren().clear();
		processNode.add(new IONode(processNode));
		processNode.editPart().refresh();
	}
}
