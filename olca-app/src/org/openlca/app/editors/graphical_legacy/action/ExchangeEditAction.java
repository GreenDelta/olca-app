package org.openlca.app.editors.graphical_legacy.action;

import java.util.Objects;

import org.eclipse.jface.action.Action;
import org.openlca.app.db.Database;
import org.openlca.app.editors.graphical_legacy.GraphEditor;
import org.openlca.app.editors.graphical_legacy.model.ExchangeNode;
import org.openlca.app.editors.graphical_legacy.model.IONode;
import org.openlca.app.editors.graphical_legacy.model.ProcessNode;
import org.openlca.app.rcp.images.Icon;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;

public class ExchangeEditAction extends Action implements GraphAction {

	private ExchangeNode exchangeNode;
	private ProcessNode processNode;

	public ExchangeEditAction() {
		setId("ExchangeEditAction");
		setImageDescriptor(Icon.EDIT.descriptor());
		setText("Edit amount");
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
		if (!ExchangeEditDialog.open(exchange))
			return;
		db.update(process);
		processNode.getChildren().clear();
		processNode.add(new IONode(processNode));
		processNode.editPart().refresh();
	}
}
