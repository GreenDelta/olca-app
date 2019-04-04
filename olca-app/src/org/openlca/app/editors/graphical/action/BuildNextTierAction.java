package org.openlca.app.editors.graphical.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.gef.commands.Command;
import org.eclipse.jface.action.Action;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.graphical.command.CommandUtil;
import org.openlca.app.editors.graphical.command.ExpansionCommand;
import org.openlca.app.editors.graphical.command.MassCreationCommand;
import org.openlca.app.editors.graphical.model.ExchangeNode;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.matrix.LinkingConfig.DefaultProviders;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;

class BuildNextTierAction extends Action implements IBuildAction {

	private final FlowDao flowDao;
	private final ProcessDao processDao;
	private List<ProcessNode> nodes;
	private ProcessType preferredType = ProcessType.UNIT_PROCESS;
	private DefaultProviders providers = DefaultProviders.ONLY;

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
	public void setProviderMethod(DefaultProviders providers) {
		this.providers = providers;
	}

	@Override
	public void run() {
		if (nodes == null || nodes.isEmpty())
			return;
		ProductSystemNode systemNode = nodes.get(0).parent();
		List<CategorizedDescriptor> providers = new ArrayList<>();
		List<ProcessLink> newConnections = new ArrayList<>();
		for (ProcessNode node : nodes)
			collectFor(node, providers, newConnections);
		Command command = MassCreationCommand.nextTier(providers, newConnections, systemNode);
		if (command == null)
			return;
		for (ProcessNode node : nodes)
			command = command.chain(ExpansionCommand.expandLeft(node));
		CommandUtil.executeCommand(command, systemNode.editor);
		systemNode.editor.setDirty(true);
	}

	private void collectFor(ProcessNode node,
			List<CategorizedDescriptor> providers,
			List<ProcessLink> newConnections) {
		for (ExchangeNode enode : getLinkCandidates(node)) {
			CategorizedDescriptor provider = findProvider(enode.exchange);
			if (provider == null)
				continue;
			if (!providers.contains(provider)) {
				providers.add(provider);
			}
			ProcessLink link = new ProcessLink();
			link.flowId = enode.exchange.flow.id;
			link.exchangeId = enode.exchange.id;
			link.processId = node.process.id;
			link.providerId = provider.id;
			link.isSystemLink = provider.type == ModelType.PRODUCT_SYSTEM;
			if (!newConnections.contains(link)) {
				newConnections.add(link);
			}
		}
	}

	private List<ExchangeNode> getLinkCandidates(ProcessNode node) {
		List<ExchangeNode> nodes = new ArrayList<>();
		for (ExchangeNode e : node.loadExchangeNodes()) {
			if (e.exchange == null)
				continue;
			if (e.parent().isConnected(e.exchange.id))
				continue; // already connected
			if (e.isWaste() && !e.exchange.isInput)
				nodes.add(e);
			else if (!e.isWaste() && e.exchange.isInput)
				nodes.add(e);
		}
		return nodes;
	}

	private CategorizedDescriptor findProvider(Exchange e) {
		if (e.flow == null)
			return null;
		if (providers == DefaultProviders.ONLY) {
			if (e.defaultProviderId == 0l)
				return null;
			return processDao.getDescriptor(e.defaultProviderId);
		}
		if (providers == DefaultProviders.PREFER
				&& e.defaultProviderId != 0L)
			return processDao.getDescriptor(e.defaultProviderId);

		List<ProcessDescriptor> providers = getProviders(e);

		ProcessDescriptor bestMatch = null;
		for (ProcessDescriptor descriptor : providers) {
			if (descriptor.processType == preferredType)
				return descriptor;
			if (bestMatch != null)
				continue;
			bestMatch = descriptor;
		}
		return bestMatch;
	}

	private List<ProcessDescriptor> getProviders(Exchange e) {
		if (e == null || e.flow == null)
			return Collections.emptyList();

		Set<Long> providerIds = null;
		if (!e.isInput) {
			providerIds = flowDao.getWhereInput(e.flow.id);
		} else {
			providerIds = flowDao.getWhereOutput(e.flow.id);
		}
		return processDao.getDescriptors(providerIds);
	}

}
