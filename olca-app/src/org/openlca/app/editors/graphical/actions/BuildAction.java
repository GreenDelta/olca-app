package org.openlca.app.editors.graphical.actions;

import static org.openlca.app.components.graphics.model.Component.CHILDREN_PROP;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.openlca.app.components.graphics.model.Side;
import org.openlca.app.db.Database;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.edit.NodeEditPart;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.requests.ExpandCollapseRequest;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.matrix.linking.LinkingConfig.PreferredType;
import org.openlca.core.matrix.linking.ProviderLinking;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.ProviderType;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.model.descriptors.RootDescriptor;

public abstract class BuildAction extends WorkbenchPartAction {

	protected final GraphEditor editor;
	protected Graph graph;
	protected Map<Exchange, Process> mapExchangeToProcess;
	protected PreferredType preferredType;
	protected ProviderLinking providers;
	protected final FlowDao flowDao = new FlowDao(Database.get());
	protected final ProcessDao processDao = new ProcessDao(Database.get());

	public BuildAction(GraphEditor part) {
		super(part);
		editor = part;
		graph = part.getModel();
	}

	public void setMapExchangeToProcess(Map<Exchange, Process> mapExchangesToProcess) {
		this.mapExchangeToProcess = mapExchangesToProcess;
	}

	public void setPreferredType(PreferredType preferredType) {
		this.preferredType = preferredType;
	}

	public void setProviderMethod(ProviderLinking providers) {
		this.providers = providers;
	}

	protected RootDescriptor findProvider(Exchange e) {
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
			if (hasPreferredType(d))
				return d;
			if (bestMatch != null)
				continue;
			bestMatch = d;
		}
		return bestMatch;
	}

	private boolean hasPreferredType(ProcessDescriptor d) {
		if (d == null)
			return false;
		return switch (d.processType) {
			case ProcessType.UNIT_PROCESS ->
					preferredType == PreferredType.UNIT_PROCESS;
			case ProcessType.LCI_RESULT ->
					preferredType == PreferredType.SYSTEM_PROCESS;
		};
	}

	protected List<ProcessDescriptor> getProviders(Exchange e) {
		if (e == null || e.flow == null)
			return Collections.emptyList();
		Set<Long> providerIds = e.isInput
				? flowDao.getWhereOutput(e.flow.id)
				: flowDao.getWhereInput(e.flow.id);
		return processDao.getDescriptors(providerIds);
	}

	protected ProcessLink createLink(
			Exchange exchange, Process process, RootDescriptor provider
	) {
		var link = new ProcessLink();
		link.exchangeId = exchange.id;
		link.flowId = exchange.flow.id;
		link.processId = process.id;
		link.providerId = provider.id;
		link.providerType = ProviderType.of(provider.type);
		return link;
	}

	protected void expandInputs() {
		var viewer = (GraphicalViewer) editor.getAdapter(GraphicalViewer.class);
		var registry = viewer.getEditPartRegistry();

		for (var process : new HashSet<>(mapExchangeToProcess.values())) {
			var part = (NodeEditPart) registry.get(graph.getNode(process.id));
			if (part == null)
				continue;

			part.getModel().setExpanded(Side.INPUT, false);
			var request = new ExpandCollapseRequest(part.getModel(), Side.INPUT, true);
			var command = part.getCommand(request);
			if (command.canExecute())
				viewer.getEditDomain().getCommandStack().execute(command);
		}
		graph.firePropertyChange(CHILDREN_PROP, null, null);
	}

}
