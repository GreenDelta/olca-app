package org.openlca.app.results;

import java.util.Collections;

import org.openlca.app.util.Labels;
import org.openlca.core.results.IResult;
import org.openlca.core.results.ProjectResult;
import org.openlca.util.Strings;

public final class Sort {

	private Sort() {
	}

	public static void sort(IResult result) {
		if (result == null)
			return;

		Collections.sort(result.getProcesses(), (p1, p2) -> {
			String name1 = Labels.name(p1);
			String name2 = Labels.name(p2);
			return Strings.compare(name1, name2);
		});

		if (result.hasFlowResults()) {
			Collections.sort(result.getFlows(), (f1, f2) -> {
				String name1 = Labels.name(f1);
				String name2 = Labels.name(f2);
				return Strings.compare(name1, name2);
			});
		}

		if (result.hasImpactResults()) {
			Collections.sort(result.getImpacts(),
					(i1, i2) -> Strings.compare(i1.name, i2.name));
		}

		if (result instanceof ProjectResult) {
			ProjectResult p = (ProjectResult) result;
			Collections.sort(p.getVariants(),
					(v1, v2) -> Strings.compare(v1.name, v2.name));
		}
	}
}
