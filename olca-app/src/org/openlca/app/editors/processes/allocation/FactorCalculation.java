package org.openlca.app.editors.processes.allocation;

import java.util.EnumMap;

import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.Process;

record FactorCalculation(
	Process process, EnumMap<AllocationMethod, Ref> map) {

	static FactorCalculation of(Process process) {
		return new FactorCalculation(process, new EnumMap<>(AllocationMethod.class));
	}

	boolean isEmpty() {
		return process == null || map == null || map.isEmpty();
	}

	void run() {
		if (isEmpty())
			return;

		var techFlows = Factors.getProviderFlows(process);

		for (var e : map.entrySet()) {
			var method = e.getKey();
			var prop = e.getValue();
			if (method == null || prop == null)
				continue;
			process.allocationFactors
				.removeIf(f -> f.method == method);
			if (techFlows.size() < 2)
				continue;


		}
	}

	private void calc() {

	}

	void bind(AllocationMethod method, Ref ref) {
		if (method == null || ref == null)
			return;
		map.put(method, ref);
	}

	/**
	 * The calculation reference for a factor calculation.
	 */
	record Ref(FlowProperty property, boolean isCosts) {

		static Ref of(FlowProperty property) {
			return new Ref(property, false);
		}

		static Ref costs() {
			return new Ref(null, true);
		}

		boolean isEmpty() {
			return property == null && !isCosts;
		}

	}

	private record Factor(Exchange techFlow, double value) {
	}
}
