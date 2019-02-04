package org.openlca.app.editors.graphical.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;

public class IONode extends Node {

	public IONode(List<Exchange> exchanges) {
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
		List<Exchange> result = new ArrayList<>();
		for (Exchange e : exchanges) {
			if (e.isInput == inputs
					&& e.flow.flowType != FlowType.ELEMENTARY_FLOW)
				result.add(e);
		}
		Collections.sort(result, new ExchangeComparator());
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

	private final class ExchangeComparator implements Comparator<Exchange> {

		@Override
		public int compare(Exchange o1, Exchange o2) {
			String s1 = o1.flow.name.toLowerCase();
			String s2 = o2.flow.name.toLowerCase();
			int length = s1.length();
			if (length > s2.length())
				length = s2.length();
			for (int i = 0; i < length; i++) {
				if (s1.charAt(i) > s2.charAt(i))
					return 1;
				if (s1.charAt(i) < s2.charAt(i))
					return -1;
			}
			return 0;
		}

	};

}
