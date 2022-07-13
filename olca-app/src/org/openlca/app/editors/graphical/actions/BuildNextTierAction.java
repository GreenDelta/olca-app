package org.openlca.app.editors.graphical.actions;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.jface.action.Action;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.graphical.model.ExchangeItem;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.app.editors.graphical.model.commands.ExpandCommand;
import org.openlca.app.editors.graphical.model.commands.MassCreationCommand;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.matrix.linking.ProviderLinking;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.model.descriptors.RootDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class BuildNextTierAction extends Action implements IBuildAction {

	private final FlowDao flowDao;
	private final ProcessDao processDao;
	private List<Node> nodes;
	private ProcessType preferredType = ProcessType.UNIT_PROCESS;
	private ProviderLinking providers = ProviderLinking.ONLY_DEFAULTS;

	public BuildNextTierAction() {
		setId(ActionIds.BUILD_NEXT_TIER);
		setText(M.BuildNextTier);
		flowDao = new FlowDao(Database.get());
		processDao = new ProcessDao(Database.get());
	}

	@Override
	public void setProcessNodes(List<Node> nodes) {
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
		Graph graph = nodes.get(0).getGraph();
		List<RootDescriptor> providers = new ArrayList<>();
		List<ProcessLink> newConnections = new ArrayList<>();
		for (Node node : nodes)
			collectFor(node, providers, newConnections);
		Command command = MassCreationCommand.nextTier(providers, newConnections, graph);
		for (Node node : nodes)
			command = command.chain(new ExpandCommand(node, Node.Side.INPUT));
		var stack = (CommandStack) graph.editor.getAdapter(CommandStack.class);
		stack.execute(command);
		graph.editor.setDirty();
	}

	private void collectFor(Node node,
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
			link.processId = node.descriptor.id;
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

	private List<ExchangeItem> getLinkCandidates(Node node) {
		List<ExchangeItem> nodes = new ArrayList<>();
		var ioPanes = node.editor.getGraphFactory().createIOPanes(node.descriptor);
		for (var pane : ioPanes.values()) {
			for (var exchangeItem : pane.getExchangesItems()) {
				if (exchangeItem.exchange == null || exchangeItem.isConnected())
					continue;
				if (exchangeItem.isWaste() && !exchangeItem.exchange.isInput)
					nodes.add(exchangeItem);
				else if (!exchangeItem.isWaste() && exchangeItem.exchange.isInput)
					nodes.add(exchangeItem);
			}
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
