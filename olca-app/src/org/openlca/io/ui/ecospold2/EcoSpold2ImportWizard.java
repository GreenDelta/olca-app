package org.openlca.io.ui.ecospold2;

import java.io.File;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.core.database.IDatabase;
import org.openlca.core.resources.ImageType;
import org.openlca.io.ecospold2.EcoSpold2Import;
import org.openlca.io.ui.FileImportPage;
import org.openlca.io.ui.SelectDatabasePage;

/**
 * Import wizard for files in the EcoSpold format version 2.
 */
public class EcoSpold2ImportWizard extends Wizard implements IImportWizard {

	private SelectDatabasePage databasePage;
	private FileImportPage importPage;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("Import EcoSpold 02 data sets");
		setDefaultPageImageDescriptor(ImageType.IMPORT_ZIP_WIZARD.getDescriptor());
	}

	@Override
	public boolean performFinish() {
		File[] files = importPage.getFiles();
		IDatabase db = databasePage.getDatabase();
		if (files == null || files.length == 0 || db == null)
			return false;
		EcoSpold2Import pi = new EcoSpold2Import(db);
		pi.run(files);
		return true;
	}

	@Override
	public void addPages() {
		databasePage = new SelectDatabasePage();
		addPage(databasePage);
		importPage = new FileImportPage(new String[] { "zip", "spold", "xml" },
				false);
		addPage(importPage);
	}

}
