package org.openlca.app.results;

import org.openlca.app.util.Labels;
import org.openlca.commons.Strings;
import org.openlca.core.results.ResultItemOrder;

public final class Sort {

	private Sort() {
	}

	public static void sort(ResultItemOrder items) {
		if (items == null)
			return;

		items.techFlows().sort((tf1, tf2) -> {
			int c = Strings.compareIgnoreCase(
				Labels.name(tf1.provider()), Labels.name(tf2.provider()));
			if (c != 0)
				return c;
			return Strings.compareIgnoreCase(
				Labels.name(tf1.flow()), Labels.name(tf2.flow()));
		});

		if (items.hasEnviFlows()) {
			items.enviFlows().sort((ef1, ef2) -> Strings.compareIgnoreCase(
				Labels.name(ef1), Labels.name(ef2)));
		}

		if (items.hasImpacts()) {
			items.impacts().sort(
				(i1, i2) -> Strings.compareIgnoreCase(i1.name, i2.name));
		}
	}

}
