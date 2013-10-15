package org.openlca.app.editors.graphical.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;

public class InputOutputNode extends Node {

	public InputOutputNode(Exchange[] exchanges) {
		List<Exchange> inputs = filter(exchanges, true);
		List<Exchange> outputs = filter(exchanges, false);

		boolean inputsAreBiggerThanOutputs = inputs.size() > outputs.size();
		int min = Math.min(inputs.size(), outputs.size());

		Collections.sort(inputs, new ExchangeComparator());
		Collections.sort(outputs, new ExchangeComparator());

		for (int i = 0; i < min; i++) {
			add(new ExchangeNode(inputs.get(i)));
			add(new ExchangeNode(outputs.get(i)));
		}

		int max = Math.max(inputs.size(), outputs.size());
		for (int i = min; i < max; i++) {
			if (inputsAreBiggerThanOutputs) {
				add(new ExchangeNode(inputs.get(i)));
				add(new ExchangeNode(null));
			} else {
				add(new ExchangeNode(null));
				add(new ExchangeNode(outputs.get(i)));
			}
		}
	}

	private List<Exchange> filter(Exchange[] exchanges, boolean inputs) {
		List<Exchange> result = new ArrayList<>();
		for (Exchange e : exchanges)
			if (e.isInput() == inputs
					&& e.getFlow().getFlowType() != FlowType.ELEMENTARY_FLOW)
				result.add(e);
		return result;
	}

	@Override
	public ProcessNode getParent() {
		return (ProcessNode) super.getParent();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<ExchangeNode> getChildren() {
		return (List<ExchangeNode>) super.getChildren();
	}

	@Override
	public String getName() {
		return null;
	}

	private final class ExchangeComparator implements Comparator<Exchange> {

		@Override
		public int compare(Exchange o1, Exchange o2) {
			String s1 = o1.getFlow().getName().toLowerCase();
			String s2 = o2.getFlow().getName().toLowerCase();
			int length = s1.length();
			if (length > s2.length())
				length = s2.length();

			for (int i = 0; i < length; i++)
				if (s1.charAt(i) > s2.charAt(i))
					return 1;
				else if (s1.charAt(i) < s2.charAt(i))
					return -1;
			return 0;
		}

	};

}
