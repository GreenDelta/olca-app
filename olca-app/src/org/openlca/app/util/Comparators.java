package org.openlca.app.util;

import java.util.Comparator;

import org.apache.commons.lang3.tuple.Pair;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.util.Strings;

public class Comparators {

	private Comparators() {
	}

	/**
	 * Returns a new comparator for flow descriptors which sorts the flow
	 * descriptors first by name and than by category.
	 */
	public static Comparator<FlowDescriptor> forFlowDescriptors() {
		return (flow1, flow2) -> {
			int c = Strings.compare(flow1.name, flow2.name);
			if (c != 0)
				return c;
			Pair<String, String> cat1 = Labels.getCategory(flow1);
			Pair<String, String> cat2 = Labels.getCategory(flow2);
			c = Strings.compare(cat1.getLeft(), cat2.getLeft());
			if (c != 0)
				return c;
			return Strings.compare(cat1.getRight(), cat2.getRight());
		};
	}

}
