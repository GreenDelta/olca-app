package org.openlca.app.editors.graphical.action;

import org.eclipse.jface.action.Action;
import org.eclipse.osgi.util.NLS;
import org.openlca.app.Messages;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.app.util.Labels;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.descriptors.ProcessDescriptor;

public class BuildSupplyChainAction extends Action {

	private ProductSystemNode node;
	private ProcessDescriptor startProcess;
	private ProcessType preferredType;

	BuildSupplyChainAction(ProcessType preferredType) {
		setId(ActionIds.BUILD_SUPPLY_CHAIN_ACTION_ID);
		String processType = Labels.processType(preferredType);
		setText(NLS.bind(Messages.Systems_Prefer, processType));

		this.preferredType = preferredType;
	}

	public void setProductSystemNode(ProductSystemNode node) {
		this.node = node;
	}

	public void setStartProcess(ProcessDescriptor process) {
		this.startProcess = process;
	}

	@Override
	public void run() {
		// TODO adjust
		// productSystemNode.getEditor().getEditor().doSave(null);
		// Runner runner = new Runner();
		// try {
		// new ProgressMonitorDialog(UI.shell()).run(true, false, runner);
		// } catch (final Exception e) {
		// log.error("Failed to complete product system. ", e);
		// }
		// OpenEditorAction openEditorAction = new OpenEditorAction();
		// openEditorAction.setModelComponent(productSystemNode.getEditor()
		// .getDatabase(), productSystemNode.getProductSystem());
		// openEditorAction.run(true);
	}

	// private class Runner implements IRunnableWithProgress {
	//
	// @Override
	// public void run(IProgressMonitor monitor)
	// throws InvocationTargetException, InterruptedException {
	// monitor.beginTask(Messages.Systems_CreatingProductSystem,
	// IProgressMonitor.UNKNOWN);
	// ProductSystem system = node.getProductSystem();
	// ProcessDescriptor start = startProcess != null ? startProcess
	// : Descriptors.toDescriptor(system.getReferenceProcess());
	// ProgressAdapter progress = new ProgressAdapter(monitor);
	// // TODO adjust
	// // IProductSystemBuilder.Factory.create(Database.get(), progress,
	// // useSystemProcesses).autoComplete(system, start);
	// progress.done();
	// }
	// }

}
