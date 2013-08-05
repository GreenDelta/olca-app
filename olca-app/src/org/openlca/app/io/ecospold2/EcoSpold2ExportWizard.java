package org.openlca.app.io.ecospold2;

import java.io.File;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.db.Database;
import org.openlca.app.io.SelectObjectsExportPage;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.io.ecospold2.ProcessExport;

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
		List<BaseDescriptor> selection = exportPage
				.getSelectedModelComponents();
		for (BaseDescriptor descriptor : selection) {
			Process process = new ProcessDao(Database.get())
					.getForId(descriptor.getId());
			ProcessExport export = new ProcessExport(process, Database.get());
			export.run(targetDir);
		}
		return true;
	}

	@Override
	public void addPages() {
		exportPage = SelectObjectsExportPage.withSelection(ModelType.PROCESS);
		exportPage.setSubDirectory("EcoSpold02");
		addPage(exportPage);
	}

}
