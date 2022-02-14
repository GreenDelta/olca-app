package org.openlca.app.wizards.io;

import java.io.File;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.ErrorReporter;
import org.openlca.core.database.IDatabase;
import org.openlca.io.ecospold2.input.EcoSpold2Import;
import org.openlca.io.ecospold2.input.ImportConfig;

/**
 * Import wizard for files in the EcoSpold format version 2.
 */
public class EcoSpold2ImportWizard extends Wizard implements IImportWizard {

	private FileImportPage filePage;

	private File initialFile;

	public static void of(File file) {
		Wizards.forImport(
				"wizard.import.ecospold2",
				(EcoSpold2ImportWizard w) -> w.initialFile = file);
	}

	public EcoSpold2ImportWizard() {
		setWindowTitle(M.ImportEcoSpold02DataSets);
		setNeedsProgressMonitor(true);
		setDefaultPageImageDescriptor(
				Icon.IMPORT_ZIP_WIZARD.descriptor());
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
	}

	@Override
	public void addPages() {
		filePage = initialFile != null
				? new FileImportPage(initialFile)
				: new FileImportPage(".zip", ".spold");
		filePage.withMultiSelection = true;
		filePage.withMappingFile = true;
		addPage(filePage);
	}

	@Override
	public boolean performFinish() {
		var imp = createImport();
		if (imp == null)
			return false;
		try {
			Database.getWorkspaceIdUpdater().beginTransaction();
			getContainer().run(
				true, true, monitor -> ImportMonitor.on(monitor).run(imp));
			return true;
		} catch (Exception e) {
			ErrorReporter.on("EcoSpold 02 import failed", e);
			return false;
		} finally {
			Database.getWorkspaceIdUpdater().endTransaction();
			Navigator.refresh();
			Cache.evictAll();
		}
	}

	private EcoSpold2Import createImport() {
		File[] files = filePage.getFiles();
		IDatabase db = Database.get();
		if (files == null || files.length == 0 || db == null)
			return null;
		ImportConfig conf = new ImportConfig(db);
		if (App.runsInDevMode()) {
			conf.checkFormulas = true;
			conf.skipNullExchanges = true;
			conf.withParameterFormulas = false;
			conf.withParameters = false;
		}
		if (filePage.flowMap != null) {
			conf.setFlowMap(filePage.flowMap);
		}
		EcoSpold2Import pi = new EcoSpold2Import(conf);
		pi.setFiles(files);
		return pi;
	}
}
