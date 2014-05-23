package org.openlca.app.editors.graphical.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.gef.commands.Command;
import org.eclipse.jface.action.Action;
import org.eclipse.osgi.util.NLS;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.editors.graphical.command.CommandUtil;
import org.openlca.app.editors.graphical.model.ExchangeNode;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.util.Labels;
import org.openlca.core.database.BaseDao;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.descriptors.ProcessDescriptor;

class BuildNextTierAction extends Action implements IBuildAction {

	private ProcessNode node;
	private ProcessType preferredType;
	private FlowDao flowDao;
	private ProcessDao processDao;
	private BaseDao<Exchange> exchangeDao;

	BuildNextTierAction(ProcessType preferredType) {
		setId(ActionIds.BUILD_NEXT_TIER);
		String processType = Labels.processType(preferredType);
		setText(NLS.bind(Messages.Systems_Prefer, processType));
		flowDao = new FlowDao(Database.get());
		processDao = new ProcessDao(Database.get());
		exchangeDao = new BaseDao<>(Exchange.class, Database.get());
		this.preferredType = preferredType;
	}

	@Override
	public void setProcessNode(ProcessNode node) {
		this.node = node;
	}

	@Override
	public void run() {
		List<ExchangeNode> exchanges = loadExchangeNodes();
		List<ProcessDescriptor> providers = new ArrayList<>();
		Map<Long, ExchangeNode> exchangeMap = new HashMap<>();
		for (ExchangeNode exchange : exchanges) {
			ProcessDescriptor provider = findProvider(exchange.getExchange());
			if (provider != null) {
				providers.add(provider);
				exchangeMap.put(provider.getId(), exchange);
			}
		}
		Command createCommand = CommandUtil.buildCreateProcessesCommand(
				providers, node.getParent());
		CommandUtil.executeCommand(createCommand, node.getParent().getEditor());
		Command connectCommand = null;
		for (ProcessDescriptor descriptor : providers)
			connectCommand = chainConnectCommand(connectCommand, descriptor,
					exchangeMap.get(descriptor.getId()));
		CommandUtil
				.executeCommand(connectCommand, node.getParent().getEditor());
		node.layout();
	}

	private List<ExchangeNode> loadExchangeNodes() {
		List<ExchangeNode> nodes = new ArrayList<>();
		for (ExchangeNode exchangeNode : node.loadExchangeNodes()) {
			if (exchangeNode.isDummy())
				continue;
			if (exchangeNode.getExchange().isInput())
				nodes.add(exchangeNode);
		}
		return nodes;
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
		Set<Long> existing = node.getParent().getProductSystem().getProcesses();
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

	private Command chainConnectCommand(Command command,
			ProcessDescriptor provider, ExchangeNode exchange) {
		Command connectCommand = CommandUtil.buildConnectProvidersCommand(
				provider, exchange);
		if (command == null)
			return connectCommand;
		return command.chain(connectCommand);
	}

}
