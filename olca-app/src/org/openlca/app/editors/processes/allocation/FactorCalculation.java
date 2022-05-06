package org.openlca.app.editors.processes.allocation;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.Process;
import org.openlca.util.Exchanges;

record FactorCalculation(Setup setup) {

	static Setup setup(Process process) {
		return new Setup(process, new EnumMap<>(AllocationMethod.class));
	}

	static Set<FlowProperty> allocationPropertiesOf(Process process) {
		if (process == null)
			return Collections.emptySet();

		var techFlows = process.exchanges.stream()
			.filter(Exchanges::isProviderFlow)
			.map(e -> e.flow)
			.toList();
		if (techFlows.isEmpty())
			return Collections.emptySet();

		var first = techFlows.get(0);
		var props = new HashSet<FlowProperty>();
		for (var propFac : first.flowPropertyFactors) {
			var next = propFac.flowProperty;
			if (next == null)
				continue;
			boolean matches = true;
			for (int i = 1; i < techFlows.size(); i++) {
				for (var otherFac : techFlows.get(i).flowPropertyFactors) {
					if (!Objects.equals(next, otherFac.flowProperty)) {
						matches = false;
						break;
					}
				}
				if (!matches)
					break;
			}
			if (matches) {
				props.add(next);
			}
		}

		return props;
	}

	void apply() {
		if (setup.isEmpty())
			return;
		// TODO
	}

	record Setup (Process process, EnumMap<AllocationMethod, FlowProperty> map) {

		boolean isEmpty() {
			return process == null || map == null || map.isEmpty();
		}

		void apply() {
			new FactorCalculation(this).apply();
		}

		void bind(AllocationMethod method, FlowProperty prop) {
			if (method == null || prop == null)
				return;
			map.put(method, prop);
		}
	}
}
