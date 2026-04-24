package org.openlca.app.tools.transfer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.openlca.commons.Res;
import org.openlca.core.model.Category;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.ProviderType;
import org.openlca.core.model.RootEntity;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.core.matrix.ProductSystemBuilder;
import org.openlca.core.matrix.linking.LinkingConfig;
import org.openlca.io.olca.TransferContext;

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
			var ctx = TransferContext.create(source, target);
			var copied = new HashMap<Long, Process>();
			var updated = new HashSet<Long>();
			var sourceLinks = linkIndexOf(sourceSystem);
			var matches = matchIndexOf(plan);

			for (var processId : plan.processIds()) {
				var origin = source.get(Process.class, processId);
				if (origin == null)
					continue;
				var existing = target.get(Process.class, origin.refId);
				var copy = ctx.resolve(origin);
				if (copy == null)
					continue;
				if (existing == null) {
					clearDefaultProviders(copy);
					updated.add(copy.id);
				}
				copied.put(processId, copy);
			}

			var refProcess = copied.get(sourceSystem.referenceProcess.id);
			if (refProcess == null) {
				return Res.error("Failed to copy the reference process");
			}

			var system = sourceSystem.copy();
			system.category = sourceSystem.category != null
				? ctx.resolve(sourceSystem.category)
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
			applyAssignments(system, copied, sourceLinks, matches, ctx, plan);

			for (var process : copied.values()) {
				if (updated.contains(process.id)) {
					target.update(process);
				}
			}

			new ProductSystemBuilder(target, new LinkingConfig()).autoComplete(system);
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
					var impact = ctx.resolve(
						source.get(ImpactCategory.class, parameter.contextId));
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
		Map<Long, ProcessLink> sourceLinks,
		Map<ProviderKey, TransferMatch> matches,
		TransferContext ctx,
		TransferPlan plan
	) {
		for (var sourceProcessId : plan.processIds()) {
			var consumer = copied.get(sourceProcessId);
			if (consumer == null)
				continue;
			var sourceProcess = plan.config().source().get(Process.class, sourceProcessId);
			if (sourceProcess == null)
				continue;
			for (var sourceExchange : sourceProcess.exchanges) {
				var exchange = consumer.getExchange(sourceExchange.internalId);
				if (exchange == null || exchange.flow == null)
					continue;
				var sourceLink = sourceLinks.get(sourceExchange.id);
				if (sourceLink == null || sourceLink.providerId == 0)
					continue;

				var match = matches.get(new ProviderKey(
					sourceLink.providerType,
					sourceLink.providerId));
				if (match == null)
					continue;

				var selected = match.selectedCandidate();
				if (selected != null) {
					assign(system, consumer.id, exchange,
						selected.providerId(),
						selected.providerType());
					continue;
				}

				if (sourceLink.providerType == ProviderType.PROCESS) {
					var copiedProvider = copied.get(sourceLink.providerId);
					if (copiedProvider != null) {
						assign(system, consumer.id, exchange,
							copiedProvider.id, ProviderType.PROCESS);
					}
					continue;
				}

				var provider = copyProvider(match, plan, ctx);
				if (provider != null) {
					assign(system, consumer.id, exchange, provider.id,
						ProviderType.of(ModelType.of(provider)));
				}
			}
		}
	}

	private static Map<Long, ProcessLink> linkIndexOf(ProductSystem system) {
		var index = new HashMap<Long, ProcessLink>();
		for (var link : system.processLinks) {
			index.put(link.exchangeId, link);
		}
		return index;
	}

	private static Map<ProviderKey, TransferMatch> matchIndexOf(TransferPlan plan) {
		var index = new HashMap<ProviderKey, TransferMatch>();
		for (var match : plan.matches()) {
			var provider = match.provider();
			if (provider != null) {
				index.put(new ProviderKey(ProviderType.of(provider.type), provider.id), match);
			}
		}
		return index;
	}

	private static RootEntity copyProvider(
		TransferMatch match,
		TransferPlan plan,
		TransferContext ctx
	) {
		var provider = match.provider();
		if (provider == null || provider.type == null)
			return null;
		var type = provider.type.getModelClass();
		var sourceProvider = plan.config().source().get(type, provider.id);
		if (!(sourceProvider instanceof RootEntity entity)) {
			return null;
		}
		return ctx.resolve(entity);
	}

	private static void assign(
		ProductSystem system,
		long processId,
		org.openlca.core.model.Exchange exchange,
		long providerId,
		byte providerType
	) {
		var link = new ProcessLink();
		link.flowId = exchange.flow.id;
		link.providerId = providerId;
		link.providerType = providerType;
		link.processId = processId;
		link.exchangeId = exchange.id;
		system.processLinks.add(link);

		if (providerType == ProviderType.PROCESS) {
			system.processes.add(providerId);
		}
	}

	private record ProviderKey(byte type, long id) {
	}
}
