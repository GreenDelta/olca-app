package org.openlca.app.editors.graphical.model;

import java.util.Collections;
import java.util.List;

import org.openlca.app.db.Database;
import org.openlca.app.util.Labels;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProductSystem;
import org.openlca.util.Strings;

public class IONode extends Node {

	/**
	 * We store the information if this node was created
	 * with elementary flows here. This makes it easier
	 * to decide if the node needs to be recreated on a
	 * refresh.
	 */
	final boolean isWithElementaryFlows;

	public IONode(ProcessNode parent) {
		super(parent.editor);
		isWithElementaryFlows = config().showElementaryFlows;
		addExchanges(parent);
	}

	private void addExchanges(ProcessNode node) {
		if (node == null || node.process == null)
			return;

		// load the exchanges
		var model = node.process;
		List<Exchange> exchanges = null;
		if (model.type == ModelType.PROCESS) {
			var process = Database.get()
				.get(Process.class, model.id);
			exchanges = process == null
				? Collections.emptyList()
				: process.exchanges;
		}
		if (model.type == ModelType.PRODUCT_SYSTEM) {
			var sys = Database.get()
				.get(ProductSystem.class, model.id);
			exchanges = sys == null || sys.referenceExchange == null
				? Collections.emptyList()
				: Collections.singletonList(sys.referenceExchange);
		}

		if (exchanges == null || exchanges.isEmpty())
			return;

		// filter and sort the exchanges
		exchanges.stream()
			.filter(e -> {
				if (e.flow == null)
					return false;
				return isWithElementaryFlows
							 || e.flow.flowType != FlowType.ELEMENTARY_FLOW;
			})
			.sorted((e1, e2) -> {
				int t1 = typeOrderOf(e1);
				int t2 = typeOrderOf(e2);
				if (t1 != t2)
					return t1 - t2;
				var name1 = Labels.name(e1.flow);
				var name2 = Labels.name(e2.flow);
				return Strings.compare(name1, name2);
			})
			.forEach(e -> add(new ExchangeNode(editor, e)));
	}

	private int typeOrderOf(Exchange e) {
		if (e == null
				|| e.flow == null
				|| e.flow.flowType == null)
			return -1;
		switch (e.flow.flowType) {
			case PRODUCT_FLOW:
				return 0;
			case WASTE_FLOW:
				return 1;
			default:
				return 2;
		}
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
