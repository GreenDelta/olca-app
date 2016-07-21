package org.openlca.app.util;

import java.util.Comparator;

import org.apache.commons.lang3.tuple.Pair;
import org.openlca.core.database.EntityCache;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.util.Strings;

public class Comparators {

	private Comparators() {
	}

	/**
	 * Returns a new comparator for flow descriptors which sorts the flow
	 * descriptors first by name and than by category.
	 */
	public static Comparator<FlowDescriptor> forFlowDescriptors(
			final EntityCache cache) {
		return new Comparator<FlowDescriptor>() {
			@Override
			public int compare(FlowDescriptor flow1, FlowDescriptor flow2) {
				int c = Strings.compare(flow1.getName(), flow2.getName());
				if (c != 0)
					return c;
				Pair<String, String> cat1 = Labels
						.getCategory(flow1, cache);
				Pair<String, String> cat2 = Labels
						.getCategory(flow2, cache);
				c = Strings.compare(cat1.getLeft(), cat2.getLeft());
				if (c != 0)
					return c;
				return Strings.compare(cat1.getRight(), cat2.getLeft());
			}
		};
	}

}
