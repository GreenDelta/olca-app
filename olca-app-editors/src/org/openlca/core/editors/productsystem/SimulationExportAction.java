package org.openlca.core.editors.productsystem;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.openlca.core.application.ApplicationProperties;
import org.openlca.core.editors.io.SimulationResultExport;
import org.openlca.core.math.SimulationResult;
import org.openlca.core.resources.ImageType;
import org.openlca.ui.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: make this a generic ExcelExportAction to be used in all editors 
class SimulationExportAction extends Action {

	private Logger log = LoggerFactory.getLogger(getClass());
	private SimulationInput input;
	private SimulationResult result;

	public SimulationExportAction() {
		setId("SimulationResultExport");
		setToolTipText("Export results to Excel");
		setImageDescriptor(ImageType.EXCEL_ICON.getDescriptor());
	}

	public void configure(SimulationInput input, SimulationResult result) {
		this.input = input;
		this.result = result;
	}

	@Override
	public void run() {
		Shell shell = UI.shell();
		if (shell == null)
			return;
		File file = getFile(shell);
		if (file == null)
			return;
		RunJob job = new RunJob(file);
		job.setUser(true);
		job.schedule();
	}

	private File getFile(Shell shell) {
		String path = getFilePath(shell);
		if (path == null)
			return null;
		File file = new File(path);
		if (!file.exists())
			return file;
		boolean b = MessageDialog.openQuestion(shell, "Overwrite?", "File "
				+ file.getName() + " already exists. "
				+ "Do you want to overwrite it?");
		if (b)
			return file;
		return null;
	}

	private String getFilePath(Shell shell) {
		FileDialog fileDialog = new FileDialog(shell, SWT.SAVE);
		fileDialog.setFileName("simulation_result.xls");
		fileDialog.setFilterExtensions(new String[] { ".xls" });
		fileDialog.setFilterPath(ApplicationProperties.PROP_EXPORT_DIRECTORY
				.getValue());
		String path = fileDialog.open();
		return path;
	}

	private class RunJob extends Job {

		File file;

		private RunJob(File file) {
			super("Simulation export");
			this.file = file;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				monitor.beginTask("Export simulation result",
						IProgressMonitor.UNKNOWN);
				SimulationResultExport export = new SimulationResultExport(
						result, input);
				export.run(file);
				monitor.done();
				return Status.OK_STATUS;
			} catch (Exception e) {
				log.error("Result export failed", e);
				return Status.CANCEL_STATUS;
			}
		}
	}

}
