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
import org.openlca.core.matrix.LongPair;
import org.openlca.core.matrix.ProductSystemBuilder;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.ProductSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class BuildSupplyChainAction extends Action implements IBuildAction {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private List<ProcessNode> nodes;
	private ProcessType preferredType = ProcessType.UNIT_PROCESS;

	BuildSupplyChainAction() {
		setId(ActionIds.BUILD_SUPPLY_CHAIN);
		setText(M.Complete);
	}

	@Override
	public void setProcessNodes(List<ProcessNode> nodes) {
		this.nodes = nodes;
	}

	void setPreferredType(ProcessType preferredType) {
		this.preferredType = preferredType;
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
			boolean preferSystems = preferredType == ProcessType.LCI_RESULT;
			ProductSystemBuilder builder = new ProductSystemBuilder(Cache.getMatrixCache(), preferSystems);
			for (ProcessNode node : nodes) {
				LongPair idPair = new LongPair(node.process.getId(), node.process.getQuantitativeReference());
				system = builder.autoComplete(system, idPair);
			}
			ProductSystemGraphEditor editor = nodes.get(0).parent().editor;
			editor.updateModel(monitor);
			Database.get().notifyUpdate(system);
		}
	}

}
