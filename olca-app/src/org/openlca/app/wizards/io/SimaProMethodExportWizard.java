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
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.io.simapro.csv.output.MethodWriter;

public class SimaProMethodExportWizard
		extends Wizard implements IExportWizard {

	private ModelSelectionPage page;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle(M.ExportLciaMethodsToASCsvFile);
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		page = ModelSelectionPage.forFile("csv", ModelType.IMPACT_METHOD);
		addPage(page);
	}

	@Override
	public boolean performFinish() {

		// convert selection to method descriptors
		var methods = page.getSelectedModels()
				.stream()
				.filter(m -> m instanceof ImpactMethodDescriptor)
				.map(m -> (ImpactMethodDescriptor) m)
				.collect(Collectors.toList());
		if (methods.isEmpty())
			return true;

		// write CSV file
		try {
			var file = page.getExportDestination();
			getContainer().run(true, true, monitor -> {
				monitor.beginTask(M.ExportingDataSetsDots, IProgressMonitor.UNKNOWN);
				new MethodWriter(Database.get(), file)
						.write(methods);
				monitor.done();
			});
			Popup.info(
					M.ExportDone,
					M.DataSetWritten + " - " + file.getName());
			return true;
		} catch (Exception e) {
			ErrorReporter.on("SimaPro CSV export failed", e);
			return false;
		}
	}
}
