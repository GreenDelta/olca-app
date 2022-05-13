package org.openlca.app.editors.graph.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.openlca.app.db.Database;
import org.openlca.app.editors.graphical.GraphConfig;
import org.openlca.app.util.Labels;
import org.openlca.core.model.*;
import org.openlca.core.model.Process;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.util.Strings;

public class GraphFactory {

	private final GraphConfig config;

	public GraphFactory(GraphConfig config) {
		this.config = config;
	}

	public Node createNode(RootDescriptor descriptor) {
		var node = new Node(descriptor);

		if (descriptor == null || descriptor.type == null)
			return node;

		var panes = createIOPanes(descriptor);
		node.addChild(panes.get("input"), 0);
		node.addChild(panes.get("output"), 1);

		return node;
	}

	public HashMap<String, IOPane> createIOPanes(RootDescriptor descriptor) {
		var panes = new HashMap<String, IOPane>();
		panes.put("input", new IOPane(true));
		panes.put("output", new IOPane(false));

		var exchanges = getExchanges(descriptor);

		// filter and sort the exchanges
		exchanges.stream()
			.filter(e -> {
				if (e.flow == null)
					return false;
				return config.showElementaryFlows
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
			.forEach(e -> {
				var key = e.isInput ? "input" : "output";
				panes.get(key).addChild(new ExchangeItem(e));
			});

		return panes;
	}

	private List<Exchange> getExchanges(RootDescriptor descriptor) {
		return switch (descriptor.type) {
			case PROCESS -> {
				var process = Database.get().get(Process.class, descriptor.id);
				yield process == null
					? Collections.emptyList()
					: process.exchanges;
			}
			case PRODUCT_SYSTEM -> {
				var system = Database.get().get(ProductSystem.class, descriptor.id);
				yield system == null || system.referenceExchange == null
					? Collections.emptyList()
					: Collections.singletonList(system.referenceExchange);
			}
			case RESULT -> {
				var result = Database.get().get(Result.class, descriptor.id);
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

}
