package org.openlca.app.wizards.io;

import java.util.ArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.util.ErrorReporter;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.io.xls.process.XlsProcessWriter;

public class ExcelExportWizard extends Wizard implements IExportWizard {

	private ModelSelectionPage page;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle(M.ExcelExport);
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		page = ModelSelectionPage.forDirectory(ModelType.PROCESS);
		addPage(page);
	}

	@Override
	public boolean performFinish() {

		var dir = page.getExportDestination();
		var processes = new ArrayList<ProcessDescriptor>();
		for (var d : page.getSelectedModels()) {
			if (d instanceof ProcessDescriptor p)
				processes.add(p);
		}

		try {
			getContainer().run(true, false, monitor -> {
				monitor.beginTask(M.Export, IProgressMonitor.UNKNOWN);
				XlsProcessWriter.of(Database.get())
						.writeAllToFolder(processes, dir);
				monitor.done();
			});
			return true;
		} catch (Exception e) {
			ErrorReporter.on("Export failed", e);
			return false;
		}
	}

}
