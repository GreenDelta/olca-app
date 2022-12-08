package org.openlca.app.editors.graphical.actions;

import java.util.HashSet;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.edit.NodeEditPart;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.requests.ExpandCollapseRequest;
import org.openlca.app.util.UI;
import org.openlca.core.matrix.ProductSystemBuilder;
import org.openlca.core.matrix.index.TechFlow;
import org.openlca.core.matrix.linking.LinkingConfig;
import org.openlca.core.matrix.linking.ProviderLinking;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.Descriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.openlca.app.tools.graphics.model.Side.INPUT;


public class BuildSupplyChainAction extends BuildAction {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final GraphEditor editor;

	private Map<Exchange, Process> mapExchangeToProcess;
	private final LinkingConfig config;

	public BuildSupplyChainAction(GraphEditor part) {
		super(part);
		editor = part;
		setId(GraphActionIds.BUILD_SUPPLY_CHAIN);
		setText(M.Complete);
		config = new LinkingConfig()
			.preferredType(ProcessType.UNIT_PROCESS)
			.providerLinking(ProviderLinking.PREFER_DEFAULTS);
	}

	@Override
	public void setMapExchangesToProcess(Map<Exchange, Process> mapExchangesToProcess) {
		this.mapExchangeToProcess = mapExchangesToProcess;
	}

	@Override
	public void setPreferredType(ProcessType preferredType) {
		config.preferredType(preferredType);
	}

	@Override
	public void setProviderMethod(ProviderLinking providerLinking) {
		config.providerLinking(providerLinking);
	}

	@Override
	public void run() {
		if (mapExchangeToProcess == null || mapExchangeToProcess.isEmpty())
			return;
		var system = editor.getProductSystem();
		try {
			if (editor.promptSaveIfNecessary())
				new ProgressMonitorDialog(
						UI.shell()).run(true, false, new Runner(system));
			var newGraph = editor.updateModel();
			editor.setDirty();
			expandInputs(newGraph);
		} catch (Exception e) {
			log.error("Failed to complete product system. ", e);
		}
	}

	private void expandInputs(Graph graph) {
		var viewer = (GraphicalViewer) editor.getAdapter(GraphicalViewer.class);
		var registry = viewer.getEditPartRegistry();

		for (var process : new HashSet<>(mapExchangeToProcess.values())) {
			var part = (NodeEditPart) registry.get(graph.getNode(process.id));
			part.getModel().setExpanded(INPUT, false);
			var request = new ExpandCollapseRequest(part.getModel(), INPUT);
			var command = part.getCommand(request);
			if (command.canExecute())
				viewer.getEditDomain().getCommandStack().execute(command);
		}
	}

	@Override
	protected boolean calculateEnabled() {
		return true;
	}

	private class Runner implements IRunnableWithProgress {

		private ProductSystem system;

		private Runner(ProductSystem system) {
			this.system = system;
		}

		@Override
		public void run(IProgressMonitor monitor) {
			monitor.beginTask(M.BuildSupplyChain, IProgressMonitor.UNKNOWN);
			var builder = new ProductSystemBuilder(Cache.getMatrixCache(), config);

			for (var exchange : mapExchangeToProcess.keySet()) {
				var process = mapExchangeToProcess.get(exchange);
				var provider = findProvider(exchange);
				var techFlow = TechFlow.of(provider, Descriptor.of(exchange.flow));
				builder.autoComplete(system, techFlow);

				var link = getLink(exchange, process, provider);
				system.processLinks.add(link);
				system.processes.add(provider.id);

				system = ProductSystemBuilder.update(Database.get(), system);
			}

			Database.get().notifyUpdate(Descriptor.of(system));
			monitor.done();
		}

	}

}
