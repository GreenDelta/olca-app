package org.openlca.app.editors.graphical.model;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.openlca.app.db.Database;
import org.openlca.app.util.Labels;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProductSystem;
import org.openlca.util.Strings;

class IONode extends Node {

	/**
	 * We store the information if this node was created
	 * with elementary flows here. This makes it easier
	 * to decide if the node needs to be recreated on a
	 * refresh.
	 */
	final boolean isWithElementaryFlows;

	IONode(ProcessNode parent) {
		super(parent.editor);
		isWithElementaryFlows = config().showElementaryFlows;
		var exchanges = exchanges(parent);
		List<Exchange> inputs = filter(exchanges, true);
		List<Exchange> outputs = filter(exchanges, false);
		int len = Math.max(inputs.size(), outputs.size());

		// creates dummy nodes if the lists are not equal in size
		// this is done because the IO table has a 2 columns grid
		// layout, we can avoid the dummy flows when we create the
		// table a bit smarter...
		for (int i = 0; i < len; i++) {
			if (i < inputs.size()) {
				add(new ExchangeNode(editor, inputs.get(i)));
			} else {
				add(new ExchangeNode(editor, null));
			}
			if (i < outputs.size()) {
				add(new ExchangeNode(editor, outputs.get(i)));
			} else {
				add(new ExchangeNode(editor, null));
			}
		}
	}

	private List<Exchange> exchanges(ProcessNode node) {
		if (node == null || node.process == null)
			return Collections.emptyList();
		var model = node.process;
		if (model.type == ModelType.PROCESS) {
			var process = Database.get()
					.get(Process.class, model.id);
			return process == null
					? Collections.emptyList()
					: process.exchanges;
		}
		if (model.type == ModelType.PRODUCT_SYSTEM) {
			var sys = Database.get()
					.get(ProductSystem.class, model.id);
			return sys == null || sys.referenceExchange == null
					? Collections.emptyList()
					: Collections.singletonList(sys.referenceExchange);
		}
		return Collections.emptyList();
	}

	private List<Exchange> filter(List<Exchange> exchanges, boolean inputs) {
		return exchanges.stream()
				.filter(e -> {
					if (e.isInput != inputs || e.flow == null)
						return false;
					return e.flow.flowType != FlowType.ELEMENTARY_FLOW
							|| config().showElementaryFlows;
				})
				.sorted((e1, e2) -> {
					var name1 = Labels.name(e1.flow);
					var name2 = Labels.name(e2.flow);
					return Strings.compare(name1, name2);
				})
				.collect(Collectors.toList());
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<ExchangeNode> getChildren() {
		return (List<ExchangeNode>) super.getChildren();
	}

}
