package org.openlca.app.editors.graphical.action;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.gef.commands.Command;
import org.eclipse.jface.action.Action;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.editors.graphical.command.CommandFactory;
import org.openlca.app.editors.graphical.command.CommandUtil;
import org.openlca.app.editors.graphical.command.ConnectionInput;
import org.openlca.app.editors.graphical.model.ExchangeNode;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.app.editors.graphical.search.ProcessLinkSearchMap;
import org.openlca.core.database.BaseDao;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.descriptors.ProcessDescriptor;

class BuildNextTierAction extends Action implements IBuildAction {

	private List<ProcessNode> nodes;
	private ProductSystemNode systemNode;
	private ProcessType preferredType = ProcessType.UNIT_PROCESS;
	private FlowDao flowDao;
	private ProcessDao processDao;
	private BaseDao<Exchange> exchangeDao;

	BuildNextTierAction() {
		setId(ActionIds.BUILD_NEXT_TIER);
		setText(Messages.BuildNextTier);
		flowDao = new FlowDao(Database.get());
		processDao = new ProcessDao(Database.get());
		exchangeDao = new BaseDao<>(Exchange.class, Database.get());
	}

	@Override
	public void setProcessNodes(List<ProcessNode> nodes) {
		this.nodes = nodes;
		if (nodes == null || nodes.isEmpty())
			return;
		this.systemNode = nodes.get(0).getParent();
	}

	void setPreferredType(ProcessType preferredType) {
		this.preferredType = preferredType;
	}

	@Override
	public void run() {
		if (nodes == null || nodes.isEmpty())
			return;
		ProductSystemNode systemNode = nodes.get(0).getParent();
		List<ProcessDescriptor> providers = new ArrayList<>();
		List<ConnectionInput> newConnections = new ArrayList<>();
		for (ProcessNode node : nodes)
			collectFor(node, providers, newConnections);
		Command command = CommandFactory.createBuildNextTierCommand(providers,
				newConnections, systemNode);
		if (command == null)
			return;
		for (ProcessNode node : nodes)
			command = command.chain(CommandFactory
					.createExpandLeftCommand(node));
		CommandUtil.executeCommand(command, systemNode.getEditor());
		systemNode.getEditor().setDirty(true);
	}

	private void collectFor(ProcessNode node,
			List<ProcessDescriptor> providers,
			List<ConnectionInput> newConnections) {
		long targetId = node.getProcess().getId();
		List<ExchangeNode> toConnect = loadExchangeNodes(node);
		for (ExchangeNode exchange : toConnect) {
			ProcessDescriptor provider = findProvider(exchange.getExchange());
			if (provider != null) {
				if (!providers.contains(provider))
					providers.add(provider);
				ConnectionInput connectionInput = new ConnectionInput(
						provider.getId(), targetId, exchange.getExchange()
								.getFlow().getId());
				if (!newConnections.contains(connectionInput))
					newConnections.add(connectionInput);
			}
		}
	}

	private List<ExchangeNode> loadExchangeNodes(ProcessNode node) {
		List<ExchangeNode> nodes = new ArrayList<>();
		for (ExchangeNode exchangeNode : node.loadExchangeNodes()) {
			if (exchangeNode.isDummy())
				continue;
			if (isAlreadyConnected(exchangeNode))
				continue;
			if (exchangeNode.getExchange().isInput())
				nodes.add(exchangeNode);
		}
		return nodes;
	}

	private boolean isAlreadyConnected(ExchangeNode exchangeNode) {
		ProcessNode processNode = exchangeNode.getParent().getParent();
		long processId = processNode.getProcess().getId();
		long flowId = exchangeNode.getExchange().getFlow().getId();
		ProcessLinkSearchMap linkSearch = processNode.getParent()
				.getLinkSearch();
		List<ProcessLink> incomingLinks = linkSearch
				.getIncomingLinks(processId);
		for (ProcessLink link : incomingLinks)
			if (link.getFlowId() == flowId)
				return true;
		return false;
	}

	private ProcessDescriptor findProvider(Exchange exchange) {
		ProcessDescriptor defaultProvider = getDefaultProvider(exchange);
		if (defaultProvider != null)
			return defaultProvider;
		List<ProcessDescriptor> providers = getProviders(exchange);
		if (providers.isEmpty())
			return null;
		long flowId = exchange.getFlow().getId();
		ProcessDescriptor matching = findMatching(providers, flowId);
		if (matching != null)
			return matching;
		ProcessDescriptor existing = findExisting(providers);
		if (existing != null)
			return existing;
		ProcessDescriptor preferred = findPreferred(providers);
		if (preferred != null)
			return preferred;
		return providers.get(0);
	}

	private ProcessDescriptor findMatching(List<ProcessDescriptor> providers,
			long flowId) {
		ProcessDescriptor candidate = null;
		for (ProcessDescriptor descriptor : providers) {
			Exchange reference = exchangeDao.getForId(descriptor
					.getQuantitativeReference());
			if (reference.getFlow().getId() != flowId)
				continue;
			if (descriptor.getProcessType() == preferredType)
				return descriptor;
			else if (candidate == null)
				candidate = descriptor;
		}
		return candidate;
	}

	private ProcessDescriptor findExisting(List<ProcessDescriptor> providers) {
		Set<Long> existing = systemNode.getProductSystem().getProcesses();
		for (ProcessDescriptor descriptor : providers)
			if (existing.contains(descriptor.getId()))
				return descriptor;
		return null;
	}

	private ProcessDescriptor findPreferred(List<ProcessDescriptor> providers) {
		for (ProcessDescriptor descriptor : providers)
			if (descriptor.getProcessType() == preferredType)
				return descriptor;
		return null;
	}

	private ProcessDescriptor getDefaultProvider(Exchange exchange) {
		if (exchange.getDefaultProviderId() == 0)
			return null;
		return processDao.getDescriptor(exchange.getDefaultProviderId());
	}

	private List<ProcessDescriptor> getProviders(Exchange exchange) {
		Set<Long> providerIds = flowDao
				.getProviders(exchange.getFlow().getId());
		return processDao.getDescriptors(providerIds);
	}

}
