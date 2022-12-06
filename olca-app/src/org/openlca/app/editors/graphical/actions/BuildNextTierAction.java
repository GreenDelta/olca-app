package org.openlca.app.editors.graphical.actions;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.edit.NodeEditPart;
import org.openlca.app.editors.graphical.model.ExchangeItem;
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

import static org.openlca.app.tools.graphics.model.Side.INPUT;

public class BuildNextTierAction extends WorkbenchPartAction
		implements IBuildAction {

	private final FlowDao flowDao;
	private final ProcessDao processDao;
	private final GraphEditor editor;
	private List<NodeEditPart> nodeParts;
	private ProcessType preferredType = ProcessType.UNIT_PROCESS;
	private ProviderLinking providers = ProviderLinking.ONLY_DEFAULTS;

	public BuildNextTierAction(GraphEditor part) {
		super(part);
		editor = part;
		setId(GraphActionIds.BUILD_NEXT_TIER);
		setText(M.BuildNextTier);
		flowDao = new FlowDao(Database.get());
		processDao = new ProcessDao(Database.get());
	}

	@Override
	public void setNodeParts(List<NodeEditPart> parts) {
		this.nodeParts = parts;
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
		if (nodeParts == null || nodeParts.isEmpty())
			return;
		var graph = editor.getModel();
		List<RootDescriptor> providers = new ArrayList<>();
		List<ProcessLink> newConnections = new ArrayList<>();
		for (var part : nodeParts)
			collectFor(part.getModel(), providers, newConnections);
		Command command = MassCreationCommand.nextTier(providers, newConnections, graph);
		for (var part : nodeParts)
			command = command.chain(new ExpandCommand(part.getModel(), INPUT));
		var stack = (CommandStack) editor.getAdapter(CommandStack.class);
		stack.execute(command);
		editor.setDirty();
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
		var ioPanes = editor.getGraphFactory().createIOPanes(node.descriptor);
		for (var pane : ioPanes.values()) {
			for (var exchangeItem : pane.getExchangeItems()) {
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

	@Override
	protected boolean calculateEnabled() {
		return false;
	}
}
