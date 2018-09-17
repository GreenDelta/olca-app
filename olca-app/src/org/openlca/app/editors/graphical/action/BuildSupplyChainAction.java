package org.openlca.app.editors.graphical.action;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.editors.graphical.ProductSystemGraphEditor;
import org.openlca.app.editors.graphical.layout.NodeLayoutStore;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.util.UI;
import org.openlca.core.matrix.LinkingConfig;
import org.openlca.core.matrix.LinkingConfig.DefaultProviders;
import org.openlca.core.matrix.LongPair;
import org.openlca.core.matrix.ProductSystemBuilder;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.Descriptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class BuildSupplyChainAction extends Action implements IBuildAction {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private List<ProcessNode> nodes;
	private final LinkingConfig config;

	BuildSupplyChainAction() {
		setId(ActionIds.BUILD_SUPPLY_CHAIN);
		setText(M.Complete);
		config = new LinkingConfig();
		config.preferredType = ProcessType.UNIT_PROCESS;
		config.providerLinking = DefaultProviders.PREFER;
	}

	@Override
	public void setProcessNodes(List<ProcessNode> nodes) {
		this.nodes = nodes;
	}

	@Override
	public void setPreferredType(ProcessType preferredType) {
		config.preferredType = preferredType;
	}

	@Override
	public void setProviderMethod(DefaultProviders providers) {
		config.providerLinking = providers;
	}

	@Override
	public void run() {
		if (nodes == null || nodes.isEmpty())
			return;
		ProductSystemGraphEditor editor = nodes.get(0).parent().editor;
		ProductSystem system = editor.getModel().getProductSystem();
		try {
			if (editor.promptSaveIfNecessary())
				new ProgressMonitorDialog(UI.shell()).run(true, false, new Runner(system));
			editor.collapse();
		} catch (Exception e) {
			log.error("Failed to complete product system. ", e);
		}
		try {
			NodeLayoutStore.loadLayout(editor.getModel());
			if (editor.getOutline() != null)
				editor.getOutline().refresh();
			editor.setDirty(true);
		} catch (Exception e) {
			log.error("Failed to apply layout to graph", e);
		}
	}

	private class Runner implements IRunnableWithProgress {

		private ProductSystem system;

		private Runner(ProductSystem system) {
			this.system = system;
		}

		@Override
		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
			monitor.beginTask(M.CreatingProductSystem, IProgressMonitor.UNKNOWN);
			ProductSystemBuilder builder = new ProductSystemBuilder(Cache.getMatrixCache(), config);
			for (ProcessNode node : nodes) {
				LongPair idPair = new LongPair(node.process.getId(), node.process.getQuantitativeReference());
				builder.autoComplete(system, idPair);
				system = builder.saveUpdates(system);
			}
			ProductSystemGraphEditor editor = nodes.get(0).parent().editor;
			editor.updateModel(monitor);
			Database.get().notifyUpdate(Descriptors.toDescriptor(system));
		}
	}

}
