package org.openlca.app.wizards.io;

import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.Popup;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.io.simapro.csv.ProcessWriter;

public class SimaProCsvExportWizard extends Wizard implements IExportWizard {

	private ModelSelectionPage page;

	@Override
	public void init(IWorkbench iWorkbench, IStructuredSelection selection) {
		setWindowTitle("Export processes to a SimaPro CSV file");
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		page = ModelSelectionPage.forFile("csv", ModelType.PROCESS);
		addPage(page);
	}

	@Override
	public boolean performFinish() {

		// convert selection to process descriptors
		var models = page.getSelectedModels()
				.stream()
				.filter(m -> m instanceof ProcessDescriptor)
				.map(m -> (ProcessDescriptor) m)
				.collect(Collectors.toList());
		if (models.isEmpty())
			return true;

		// write CSV file
		try {
			var file = page.getExportDestination();
			getContainer().run(true, true, monitor -> {
				monitor.beginTask(
						M.ExportingProcesses,
						IProgressMonitor.UNKNOWN);
				var exp = new ProcessWriter(Database.get());
				exp.write(models, page.getExportDestination());
				monitor.done();
			});
			Popup.info("Export done", "Wrote "
					+ models.size() + " data sets to " + file.getName());
			return true;
		} catch (Exception e) {
			ErrorReporter.on("SimaPro CSV export failed", e);
			return false;
		}
	}
}
