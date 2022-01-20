package org.openlca.app.wizards.io;

import java.io.File;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.ErrorReporter;
import org.openlca.io.xls.process.input.ExcelImport;

public class ExcelImportWizard extends Wizard implements IImportWizard {

	private FileImportPage importPage;

	private File initialFile;

	public static void of(File file) {
		Wizards.forImport(
				"wizard.import.excel",
				(ExcelImportWizard w) -> w.initialFile = file);
	}

	public ExcelImportWizard() {
		setWindowTitle(M.ProcessExcelImportDescription);
		setDefaultPageImageDescriptor(Icon.IMPORT_ZIP_WIZARD.descriptor());
		setNeedsProgressMonitor(true);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
	}

	@Override
	public void addPages() {
		importPage = initialFile != null
				? new FileImportPage(initialFile)
				: new FileImportPage("xlsx", "xls");
		importPage.withMultiSelection = true;
		addPage(importPage);
	}

	@Override
	public boolean performFinish() {
		File[] files = importPage.getFiles();
		if (files == null)
			return false;
		try {
			Database.getWorkspaceIdUpdater().beginTransaction();
			doRun(files);
			return true;
		} catch (Exception e) {
			ErrorReporter.on("Failed to import Excel file(s)", e);
			return false;
		} finally {
			Database.getWorkspaceIdUpdater().endTransaction();
			Navigator.refresh();
			Cache.evictAll();
		}
	}

	private void doRun(File[] files) throws Exception {
		getContainer().run(true, true, monitor -> {
			monitor.beginTask(M.Import, files.length);
			for (File file : files) {
				monitor.subTask(file.getName());
				ExcelImport importer = new ExcelImport(file, Database.get());
				importer.run();
				monitor.worked(1);
			}
			monitor.done();
		});
	}
}
