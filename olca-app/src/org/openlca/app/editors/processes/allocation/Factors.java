package org.openlca.app.editors.processes.allocation;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.Process;
import org.openlca.util.Exchanges;

final class Factors {

	private Factors() {
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

	/**
	 * Provider flows are product outputs are waste inputs. For each of such a
	 * flow a mono-functional process can be created when applying allocation
	 * factors.
	 */
	static List<Exchange> getProviderFlows(Process p) {
		if (p == null)
			return Collections.emptyList();
		return p.exchanges.stream()
			.filter(Exchanges::isProviderFlow)
			.collect(Collectors.toList());
	}

	/**
	 * Non-provider flows are product inputs, waste outputs and all elementary
	 * flows that are partitioned when applying allocation factors to create
	 * mono-functional processes.
	 */
	static List<Exchange> getNonProviderFlows(Process p) {
		if (p == null)
			return Collections.emptyList();
		return p.exchanges.stream()
			.filter(e -> !Exchanges.isProviderFlow(e))
			.collect(Collectors.toList());
	}

	static Calculation calculation(Process process) {
		return new Calculation(process, new EnumMap<>(AllocationMethod.class));
	}


	record Calculation(
		Process process, EnumMap<AllocationMethod, FlowProperty> map) {

		boolean isEmpty() {
			return process == null || map == null || map.isEmpty();
		}

		void apply() {
			if (isEmpty())
				return;

			var techFlows = getProviderFlows(process);

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

		void bind(AllocationMethod method, FlowProperty prop) {
			if (method == null || prop == null)
				return;
			map.put(method, prop);
		}
	}

	private record Factor (Exchange techFlow, double value) {};

}
