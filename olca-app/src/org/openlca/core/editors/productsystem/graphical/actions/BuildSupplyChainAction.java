package org.openlca.core.editors.productsystem.graphical.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;
import org.openlca.app.Messages;
import org.openlca.app.component.ProgressAdapter;
import org.openlca.app.util.UI;
import org.openlca.core.application.actions.OpenEditorAction;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.IProductSystemBuilder;
import org.openlca.core.editors.productsystem.graphical.model.ProductSystemNode;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProductSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action used by the graphical editor to build the supply chain of a selected
 * process
 */
public class BuildSupplyChainAction extends Action {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private ProductSystemNode productSystemNode;
	private Process startProcess;
	private boolean useSystemProcesses;

	public BuildSupplyChainAction(boolean useSystemProcesses) {
		setId("graphical.actions.BuildSupplyChainAction");
		this.useSystemProcesses = useSystemProcesses;
	}

	@Override
	public String getText() {
		String type = useSystemProcesses ? Messages.Common_SystemProcess
				: Messages.Common_UnitProcess;
		return NLS.bind(Messages.Systems_Prefer, type);
	}

	public void setProductSystemNode(final ProductSystemNode productSystemNode) {
		this.productSystemNode = productSystemNode;
	}

	public void setStartProcess(final Process process) {
		this.startProcess = process;
	}

	@Override
	public void run() {
		productSystemNode.getEditor().getEditor().doSave(null);
		Runner runner = new Runner();
		try {
			new ProgressMonitorDialog(UI.shell()).run(true, false, runner);
		} catch (final Exception e) {
			log.error("Failed to complete product system. ", e);
		}
		OpenEditorAction openEditorAction = new OpenEditorAction();
		openEditorAction.setModelComponent(productSystemNode.getEditor()
				.getDatabase(), productSystemNode.getProductSystem());
		openEditorAction.run(true);
	}

	private class Runner implements IRunnableWithProgress {

		@Override
		public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
			monitor.beginTask(Messages.Systems_CreatingProductSystem,
					IProgressMonitor.UNKNOWN);
			ProductSystem system = productSystemNode.getProductSystem();
			Process start = startProcess != null ? startProcess : system
					.getReferenceProcess();
			IDatabase db = productSystemNode.getEditor().getDatabase();
			ProgressAdapter progress = new ProgressAdapter(monitor);
			IProductSystemBuilder.Factory.create(db, progress,
					useSystemProcesses).autoComplete(system, start);
			progress.done();
		}
	}

}
