package org.openlca.app.editors.graphical_legacy.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.gef.commands.Command;
import org.eclipse.jface.action.Action;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.graphical_legacy.command.Commands;
import org.openlca.app.editors.graphical_legacy.command.ExpansionCommand;
import org.openlca.app.editors.graphical_legacy.command.MassCreationCommand;
import org.openlca.app.editors.graphical_legacy.model.ExchangeNode;
import org.openlca.app.editors.graphical_legacy.model.ProcessNode;
import org.openlca.app.editors.graphical_legacy.model.ProductSystemNode;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.matrix.linking.ProviderLinking;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.model.descriptors.RootDescriptor;

class BuildNextTierAction extends Action implements IBuildAction {

	private final FlowDao flowDao;
	private final ProcessDao processDao;
	private List<ProcessNode> nodes;
	private ProcessType preferredType = ProcessType.UNIT_PROCESS;
	private ProviderLinking providers = ProviderLinking.ONLY_DEFAULTS;

	BuildNextTierAction() {
		setId(ActionIds.BUILD_NEXT_TIER);
		setText(M.BuildNextTier);
		flowDao = new FlowDao(Database.get());
		processDao = new ProcessDao(Database.get());
	}

	@Override
	public void setProcessNodes(List<ProcessNode> nodes) {
		this.nodes = nodes;
	}

	@Override
	public void setPreferredType(ProcessType preferredType) {
		this.preferredType = preferredType;
	}

	@Override
	public void setProviderMethod(ProviderLinking providers) {
		this.providers = providers;
	}

	@Override
	public void run() {
		if (nodes == null || nodes.isEmpty())
			return;
		ProductSystemNode systemNode = nodes.get(0).parent();
		List<RootDescriptor> providers = new ArrayList<>();
		List<ProcessLink> newConnections = new ArrayList<>();
		for (ProcessNode node : nodes)
			collectFor(node, providers, newConnections);
		Command command = MassCreationCommand.nextTier(providers, newConnections, systemNode);
		for (ProcessNode node : nodes)
			command = command.chain(ExpansionCommand.expandLeft(node));
		Commands.executeCommand(command, systemNode.editor);
		systemNode.editor.setDirty();
	}

	private void collectFor(ProcessNode node,
		List<RootDescriptor> providers, List<ProcessLink> newConnections) {
		for (var eNode : getLinkCandidates(node)) {
			var provider = findProvider(eNode.exchange);
			if (provider == null)
				continue;
			if (!providers.contains(provider)) {
				providers.add(provider);
			}
			var link = new ProcessLink();
			link.flowId = eNode.exchange.flow.id;
			link.exchangeId = eNode.exchange.id;
			link.processId = node.process.id;
			link.providerId = provider.id;
			if (provider.type == null) {
				link.providerType = ProcessLink.ProviderType.PROCESS;
			} else {
				link.providerType = switch (provider.type) {
					case PRODUCT_SYSTEM -> ProcessLink.ProviderType.SUB_SYSTEM;
					case RESULT -> ProcessLink.ProviderType.RESULT;
					default -> ProcessLink.ProviderType.PROCESS;
				};
			}

			if (!newConnections.contains(link)) {
				newConnections.add(link);
			}
		}
	}

	private List<ExchangeNode> getLinkCandidates(ProcessNode node) {
		List<ExchangeNode> nodes = new ArrayList<>();
		for (ExchangeNode e : node.loadExchangeNodes()) {
			if (e.exchange == null || e.isConnected())
				continue;
			if (e.isWaste() && !e.exchange.isInput)
				nodes.add(e);
			else if (!e.isWaste() && e.exchange.isInput)
				nodes.add(e);
		}
		return nodes;
	}

	private RootDescriptor findProvider(Exchange e) {
		if (e.flow == null)
			return null;
		if (providers == ProviderLinking.ONLY_DEFAULTS) {
			if (e.defaultProviderId == 0L)
				return null;
			return processDao.getDescriptor(e.defaultProviderId);
		}
		if (providers == ProviderLinking.PREFER_DEFAULTS
			&& e.defaultProviderId != 0L)
			return processDao.getDescriptor(e.defaultProviderId);

		ProcessDescriptor bestMatch = null;
		for (var d : getProviders(e)) {
			if (d.processType == preferredType)
				return d;
			if (bestMatch != null)
				continue;
			bestMatch = d;
		}
		return bestMatch;
	}

	private List<ProcessDescriptor> getProviders(Exchange e) {
		if (e == null || e.flow == null)
			return Collections.emptyList();
		Set<Long> providerIds = e.isInput
			? flowDao.getWhereOutput(e.flow.id)
			: flowDao.getWhereInput(e.flow.id);
		return processDao.getDescriptors(providerIds);
	}

}
