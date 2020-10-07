package org.openlca.app.editors.graphical.action;

import org.eclipse.jface.action.Action;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.model.ExchangeNode;
import org.openlca.app.rcp.images.Icon;
import org.openlca.util.Strings;

public class EditExchangeAction extends Action {

	private final GraphEditor editor;
	private final ExchangeNode node;

	public EditExchangeAction(GraphEditor editor, ExchangeNode node) {
		this.editor = editor;
		this.node = node;
		setId("EditExchangeAction");
		setImageDescriptor(Icon.FORMULA.descriptor());
		if (node.exchange == null || node.exchange.flow == null) {
			setText("Edit amount");
		} else {
			setText("Edit amount of "
					+ Strings.cut(node.exchange.flow.name, 30));
		}
	}

	@Override
	public void run() {
	}
}
