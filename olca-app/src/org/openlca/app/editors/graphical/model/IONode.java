package org.openlca.app.editors.graphical.model;

import java.util.ArrayList;
import java.util.List;

import org.openlca.app.util.Labels;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;
import org.openlca.util.Strings;

class IONode extends Node {

	IONode(List<Exchange> exchanges) {
		List<Exchange> inputs = filter(exchanges, true);
		List<Exchange> outputs = filter(exchanges, false);
		int len = Math.max(inputs.size(), outputs.size());
		// creates dummy nodes if the lists are not equal in size
		for (int i = 0; i < len; i++) {
			if (i < inputs.size()) {
				add(new ExchangeNode(inputs.get(i)));
			} else {
				add(new ExchangeNode(null));
			}
			if (i < outputs.size()) {
				add(new ExchangeNode(outputs.get(i)));
			} else {
				add(new ExchangeNode(null));
			}
		}
	}

	private List<Exchange> filter(List<Exchange> exchanges, boolean inputs) {
		var result = new ArrayList<Exchange>();
		for (var e : exchanges) {
			if (e.isInput == inputs
					&& e.flow.flowType != FlowType.ELEMENTARY_FLOW)
				result.add(e);
		}
		result.sort((e1, e2) -> {
			var name1 = Labels.name(e1.flow);
			var name2 = Labels.name(e2.flow);
			return Strings.compare(name1, name2);
		});
		return result;
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
