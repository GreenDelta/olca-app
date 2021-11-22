package org.openlca.app.wizards.io;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.ErrorReporter;
import org.openlca.io.simapro.csv.input.SimaProCsvImport;

public class SimaProCsvImportWizard extends Wizard implements IImportWizard {

	private FileImportPage filePage;
	private File initialFile;

	public static void of(File file) {
		Wizards.forImport("wizard.import.csv",
				(SimaProCsvImportWizard w) -> w.initialFile = file);
	}

	public SimaProCsvImportWizard() {
		setNeedsProgressMonitor(true);
		setWindowTitle(M.SimaProCSVImport);
		setDefaultPageImageDescriptor(Icon.IMPORT_ZIP_WIZARD.descriptor());
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
	}

	@Override
	public void addPages() {
		filePage = initialFile != null
				? new FileImportPage(initialFile)
				: new FileImportPage("csv");
		filePage.withMappingFile = true;
		filePage.withMultiSelection = true;
		addPage(filePage);
	}

	@Override
	public boolean performFinish() {
		File[] files = filePage.getFiles();
		var db = Database.get();
		if (files == null || files.length == 0 || db == null)
			return false;
		var importer = new SimaProCsvImport(db, files);
		if (filePage.flowMap != null) {
			importer.withFlowMap(filePage.flowMap);
		}
		try {
			Database.getIndexUpdater().beginTransaction();
			getContainer().run(true, true, monitor -> {
				monitor.beginTask(M.Import, IProgressMonitor.UNKNOWN);
				var handler = new ImportHandler(monitor);
				handler.run(importer);
			});
			Navigator.refresh();
		} catch (Exception e) {
			ErrorReporter.on("SimaPro CSV import failed", e);
		} finally {
			Database.getIndexUpdater().endTransaction();
		}
		return true;
	}

}
