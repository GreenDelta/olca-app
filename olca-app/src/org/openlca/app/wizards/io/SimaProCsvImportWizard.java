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
import org.openlca.core.database.IDatabase;
import org.openlca.io.simapro.csv.input.SimaProCsvImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimaProCsvImportWizard extends Wizard implements IImportWizard {

	private Logger log = LoggerFactory.getLogger(getClass());
	private FileImportPage importPage;

	public SimaProCsvImportWizard() {
		setNeedsProgressMonitor(true);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle(M.SimaProCSVImport);
		setDefaultPageImageDescriptor(Icon.IMPORT_ZIP_WIZARD
				.descriptor());
	}

	@Override
	public boolean performFinish() {
		File[] files = importPage.getFiles();
		IDatabase db = Database.get();
		if (files == null || files.length == 0 || db == null)
			return false;
		SimaProCsvImport importer = new SimaProCsvImport(db, files);
		try {
			Database.getIndexUpdater().beginTransaction();
			getContainer().run(true, true, monitor -> {
				monitor.beginTask(M.Import, IProgressMonitor.UNKNOWN);
				ImportHandler handler = new ImportHandler(monitor);
				handler.run(importer);
			});
			Navigator.refresh();
		} catch (Exception e) {
			log.error("SimaPro CSV import failed", e);
		} finally {
			Database.getIndexUpdater().endTransaction();
		}
		return true;
	}

	@Override
	public void addPages() {
		importPage = new FileImportPage(new String[] { "csv" }, true);
		addPage(importPage);
	}

}
