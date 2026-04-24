package org.openlca.app.tools.transfer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openlca.commons.Res;
import org.openlca.core.model.Category;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.ProviderType;
import org.openlca.io.olca.TransferContext;
import org.openlca.io.olca.TransferPolicy;

final class TransferExecutor {

	private TransferExecutor() {
	}

	static Res<ProductSystem> execute(TransferPlan plan) {
		if (plan == null || plan.config() == null || plan.config().system() == null) {
			return Res.error("No transfer plan available");
		}

		var config = plan.config();
		var source = config.source();
		var target = config.target();
		var sourceSystem = config.system();
		if (sourceSystem.referenceProcess == null) {
			return Res.error("The selected product system has no reference process");
		}

		try {
			var processIds = new HashSet<Long>(plan.processIds());
			var ctx = TransferContext.create(source, target,
				new ForegroundPolicy(processIds, sourceSystem.id));
			var copied = new HashMap<Long, Process>();

			for (var processId : plan.processIds()) {
				var origin = source.get(Process.class, processId);
				if (origin == null)
					continue;
				var copy = ctx.getTransfer(Process.class).sync(origin);
				if (copy == null)
					continue;
				clearDefaultProviders(copy);
				copied.put(processId, copy);
			}

			var refProcess = copied.get(sourceSystem.referenceProcess.id);
			if (refProcess == null) {
				return Res.error("Failed to copy the reference process");
			}

			var system = sourceSystem.copy();
			system.category = sourceSystem.category != null
				? ctx.getTransfer(Category.class).sync(sourceSystem.category)
				: null;
			system.referenceProcess = refProcess;
			system.referenceExchange = sourceSystem.referenceExchange != null
				? refProcess.getExchange(sourceSystem.referenceExchange.internalId)
				: null;
			if (system.referenceExchange != null) {
				system.targetFlowPropertyFactor = system.referenceExchange.flowPropertyFactor;
				system.targetUnit = system.referenceExchange.unit;
			} else {
				system.targetFlowPropertyFactor = null;
				system.targetUnit = null;
			}

			remapParameterSets(system, copied, source, ctx);
			remapAnalysisGroups(system, copied);
			system.processLinks.clear();
			system.processes.clear();
			for (var process : copied.values()) {
				system.processes.add(process.id);
			}
			applyAssignments(system, copied, plan);

			for (var process : copied.values()) {
				target.update(process);
			}
			system = target.insert(system);
			return Res.ok(system);
		} catch (Exception e) {
			return Res.error("Failed to transfer the product system", e);
		}
	}

	private static void clearDefaultProviders(Process process) {
		for (var exchange : process.exchanges) {
			exchange.defaultProviderId = 0;
			exchange.defaultProviderType = ProviderType.PROCESS;
		}
	}

	private static void remapParameterSets(
		ProductSystem system,
		Map<Long, Process> copied,
		org.openlca.core.database.IDatabase source,
		TransferContext ctx
	) {
		for (var set : system.parameterSets) {
			for (var parameter : set.parameters) {
				if (parameter.contextId == null || parameter.contextType == null)
					continue;
				if (parameter.contextType == ModelType.PROCESS) {
					var process = copied.get(parameter.contextId);
					parameter.contextId = process != null ? process.id : null;
					continue;
				}
				if (parameter.contextType == ModelType.IMPACT_CATEGORY) {
					var impact = ctx.getTransfer(ImpactCategory.class)
						.sync(source.get(ImpactCategory.class, parameter.contextId));
					parameter.contextId = impact != null ? impact.id : null;
				}
			}
		}
	}

	private static void remapAnalysisGroups(
		ProductSystem system,
		Map<Long, Process> copied
	) {
		for (var group : system.analysisGroups) {
			var mapped = new HashSet<Long>();
			for (var processId : group.processes) {
				var process = copied.get(processId);
				if (process != null) {
					mapped.add(process.id);
				}
			}
			group.processes.clear();
			group.processes.addAll(mapped);
		}
	}

	private static void applyAssignments(
		ProductSystem system,
		Map<Long, Process> copied,
		TransferPlan plan
	) {
		for (var match : plan.matches()) {
			var consumer = copied.get(match.sourceProcessId());
			if (consumer == null)
				continue;
			var exchange = consumer.getExchange(match.sourceExchangeInternalId());
			if (exchange == null || exchange.flow == null)
				continue;

			var selected = match.selectedCandidate();
			if (selected != null) {
				assign(system, exchange,
					selected.providerId(),
					selected.providerType());
				continue;
			}

			if (match.sourceProviderType() != ProviderType.PROCESS
				|| match.sourceProviderId() == 0) {
				continue;
			}

			var copiedProvider = copied.get(match.sourceProviderId());
			if (copiedProvider != null) {
				assign(system, exchange, copiedProvider.id, ProviderType.PROCESS);
			}
		}
	}

	private static void assign(
		ProductSystem system,
		org.openlca.core.model.Exchange exchange,
		long providerId,
		byte providerType
	) {
		exchange.defaultProviderId = providerId;
		exchange.defaultProviderType = providerType;

		var link = new ProcessLink();
		link.flowId = exchange.flow.id;
		link.providerId = providerId;
		link.providerType = providerType;
		link.processId = exchange.owner != null ? exchange.owner.id : 0;
		link.exchangeId = exchange.id;
		system.processLinks.add(link);

		if (providerType == ProviderType.PROCESS) {
			system.processes.add(providerId);
		}
	}

	private record ForegroundPolicy(Set<Long> processIds, long systemId)
		implements TransferPolicy {

		@Override
		public boolean mapToExisting(ModelType type, long sourceId) {
			return !isForeground(type, sourceId);
		}

		@Override
		public boolean keepRefId(ModelType type, long sourceId) {
			return !isForeground(type, sourceId);
		}

		private boolean isForeground(ModelType type, long sourceId) {
			return switch (type) {
				case PROCESS -> processIds.contains(sourceId);
				case PRODUCT_SYSTEM -> sourceId == systemId;
				default -> false;
			};
		}
	}
}
