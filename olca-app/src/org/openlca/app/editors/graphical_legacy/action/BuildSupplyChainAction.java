package org.openlca.app.editors.graphical_legacy.action;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.editors.graphical_legacy.GraphEditor;
import org.openlca.app.editors.graphical_legacy.model.ProcessNode;
import org.openlca.app.util.UI;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.matrix.ProductSystemBuilder;
import org.openlca.core.matrix.index.TechFlow;
import org.openlca.core.matrix.linking.LinkingConfig;
import org.openlca.core.matrix.linking.ProviderLinking;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.Descriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class BuildSupplyChainAction extends Action implements IBuildAction {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private List<ProcessNode> nodes;
	private final LinkingConfig config;

	BuildSupplyChainAction() {
		setId(ActionIds.BUILD_SUPPLY_CHAIN);
		setText(M.Complete);
		config = new LinkingConfig()
			.preferredType(ProcessType.UNIT_PROCESS)
			.providerLinking(ProviderLinking.PREFER_DEFAULTS);
	}

	@Override
	public void setProcessNodes(List<ProcessNode> nodes) {
		this.nodes = nodes;
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
		if (nodes == null || nodes.isEmpty())
			return;
		GraphEditor editor = nodes.get(0).parent().editor;
		ProductSystem system = editor.getModel().getProductSystem();
		try {
			if (editor.promptSaveIfNecessary())
				new ProgressMonitorDialog(UI.shell()).run(true, false, new Runner(system));
			editor.collapse();
		} catch (Exception e) {
			log.error("Failed to complete product system. ", e);
		}
		try {
			if (editor.getOutline() != null)
				editor.getOutline().refresh();
			editor.setDirty();
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
		public void run(IProgressMonitor monitor) {
			monitor.beginTask(M.CreatingProductSystem, IProgressMonitor.UNKNOWN);
			var builder = new ProductSystemBuilder(Cache.getMatrixCache(), config);
			for (ProcessNode node : nodes) {
				var dao = new ProcessDao(Database.get());
				var p = dao.getForId(node.process.id);
				builder.autoComplete(system, TechFlow.of(p));
				system = ProductSystemBuilder.update(Database.get(), system);
			}
			GraphEditor editor = nodes.get(0).parent().editor;
			editor.updateModel(monitor);
			Database.get().notifyUpdate(Descriptor.of(system));
		}
	}

}
