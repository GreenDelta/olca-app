package org.openlca.io.ui.ecospold2;

import java.io.File;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.core.model.Process;
import org.openlca.io.ecospold2.ProcessExport;
import org.openlca.io.ui.ObjectWrapper;
import org.openlca.io.ui.SelectObjectsExportPage;

public class EcoSpold2ExportWizard extends Wizard implements IExportWizard {

	private SelectObjectsExportPage exportPage;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("EcoSpold 2 Export");
		setNeedsProgressMonitor(true);
	}

	@Override
	public boolean performFinish() {
		File targetDir = exportPage.getExportDestination();
		List<ObjectWrapper> wrapper = exportPage.getSelectedModelComponents();
		for (ObjectWrapper w : wrapper) {
			ProcessExport export = new ProcessExport(
					(Process) w.getModelComponent(), w.getDatabase());
			export.run(targetDir);
		}
		return true;
	}

	@Override
	public void addPages() {
		exportPage = new SelectObjectsExportPage(false,
				SelectObjectsExportPage.PROCESS, false, "EcoSpold02");
		addPage(exportPage);
	}

}
