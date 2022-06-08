package org.openlca.app.editors.graphical_legacy.model;

import java.util.Collections;
import java.util.List;

import org.openlca.app.db.Database;
import org.openlca.app.util.Labels;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.Result;
import org.openlca.util.Strings;

/**
 * An {@link IONode} represents a list of exchanges to be displayed. Each
 * exchange is represented by a {@link ExchangeNode}.
 */
public class IONode extends Node {

	/**
	 * We store the information if this node was created
	 * with elementary flows here. This makes it easier
	 * to decide if the node needs to be recreated on a
	 * refresh.
	 */
	final boolean isWithElementaryFlows;

	private final ProcessNode parent;

	public IONode(ProcessNode parent) {
		super(parent.editor);
		this.parent = parent;
		isWithElementaryFlows = config().showElementaryFlows;
		addExchanges(parent);
	}

	@Override
	public ProcessNode parent() {
		return parent;
	}

	private void addExchanges(ProcessNode node) {
		if (node == null || node.process == null || node.process.type == null)
			return;
		var model = node.process;

		List<Exchange> exchanges = switch (model.type) {
			case PROCESS -> {
				var process = Database.get().get(Process.class, model.id);
				yield process == null
					? Collections.emptyList()
					: process.exchanges;
			}
			case PRODUCT_SYSTEM -> {
				var system = Database.get().get(ProductSystem.class, model.id);
				yield system == null || system.referenceExchange == null
					? Collections.emptyList()
					: Collections.singletonList(system.referenceExchange);
			}
			case RESULT -> {
				var result = Database.get().get(Result.class, model.id);
				var refFlow = result.referenceFlow;
				if (refFlow == null)
					yield Collections.emptyList();
				var e = new Exchange();
				e.isInput = refFlow.isInput;
				e.flow = refFlow.flow;
				e.amount = refFlow.amount;
				e.flowPropertyFactor = refFlow.flowPropertyFactor;
				e.unit = refFlow.unit;
				e.location = refFlow.location;
				yield Collections.singletonList(e);
			}
			default -> Collections.emptyList();
		};

		if (exchanges.isEmpty())
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
		return switch (e.flow.flowType) {
			case PRODUCT_FLOW -> 0;
			case WASTE_FLOW -> 1;
			default -> 2;
		};
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<ExchangeNode> getChildren() {
		return (List<ExchangeNode>) super.getChildren();
	}

}
