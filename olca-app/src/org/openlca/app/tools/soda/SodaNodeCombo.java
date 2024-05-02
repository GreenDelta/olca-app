package org.openlca.app.tools.soda;

import java.util.List;
import java.util.function.Consumer;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;

class SodaNodeCombo {

	private final List<SodaNode> nodes;
	private Consumer<SodaNode> consumer;

	private SodaNodeCombo() {
		nodes = SodaNode.getAllKnown();
	}

	static SodaNodeCombo create(Composite comp, FormToolkit tk) {
		var combo = new SodaNodeCombo();
		combo.render(comp, tk);
		return combo;
	}

	private void render(Composite comp, FormToolkit tk) {
		var combo = UI.labeledCombo(comp, tk, "Known hosts");
		var items = new String[nodes.size() + 1];
		items[0] = "";
		for (int i = 0; i < nodes.size(); i++) {
			var node = nodes.get(i);
			items[i + 1] = node.name();
		}

		combo.setItems(items);
		combo.select(0);
		Controls.onSelect(combo, $ -> {
			if (consumer == null)
				return;
			int i = combo.getSelectionIndex();
			var node = i == 0
					? new SodaNode("", "", false)
					: nodes.get(i - 1);
			consumer.accept(node);
		});
	}

	void onSelect(Consumer<SodaNode> fn) {
		this.consumer = fn;
	}
}
