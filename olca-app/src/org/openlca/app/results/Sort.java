package org.openlca.app.results;

import org.openlca.app.util.Labels;
import org.openlca.core.results.IResult;
import org.openlca.core.results.ResultItemView;
import org.openlca.util.Strings;

public final class Sort {

	private Sort() {
	}

	public static void sort(IResult result) {
		if (result == null)
			return;

		result.getProcesses().sort((p1, p2) -> {
			String name1 = Labels.name(p1);
			String name2 = Labels.name(p2);
			return Strings.compare(name1, name2);
		});

		if (result.hasEnviFlows()) {
			result.getFlows().sort((f1, f2) -> {
				String name1 = Labels.name(f1);
				String name2 = Labels.name(f2);
				return Strings.compare(name1, name2);
			});
		}

		if (result.hasImpacts()) {
			result.getImpacts().sort(
				(i1, i2) -> Strings.compare(i1.name, i2.name));
		}
	}

	public static void sort(ResultItemView items) {
		if (items == null)
			return;

		items.techFlows().sort((tf1, tf2) -> {
			int c = Strings.compare(
				Labels.name(tf1.provider()), Labels.name(tf2.provider()));
			if (c != 0)
				return c;
			return Strings.compare(
				Labels.name(tf1.flow()), Labels.name(tf2.flow()));
		});

		if (items.hasEnviFlows()) {
			items.enviFlows().sort((ef1, ef2) -> Strings.compare(
				Labels.name(ef1), Labels.name(ef2)));
		}

		if (items.hasImpacts()) {
			items.impacts().sort(
				(i1, i2) -> Strings.compare(i1.name, i2.name));
		}
	}

}
