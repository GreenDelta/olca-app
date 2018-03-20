package org.openlca.app.wizards.io;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
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
import org.openlca.core.database.IDatabase;
import org.openlca.io.ecospold2.input.EcoSpold2Import;
import org.openlca.io.ecospold2.input.ImportConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Import wizard for files in the EcoSpold format version 2.
 */
public class EcoSpold2ImportWizard extends Wizard implements IImportWizard {

	private Logger log = LoggerFactory.getLogger(getClass());
	private FileImportPage importPage;

	public EcoSpold2ImportWizard() {
		setNeedsProgressMonitor(true);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle(M.ImportEcoSpold02DataSets);
		setDefaultPageImageDescriptor(Icon.IMPORT_ZIP_WIZARD
				.descriptor());
	}

	@Override
	public boolean performFinish() {
		EcoSpold2Import pi = createImport();
		if (pi == null)
			return false;
		try {
			Database.getIndexUpdater().beginTransaction();
			getContainer().run(true, true, (monitor) -> {
				monitor.beginTask(M.Import, IProgressMonitor.UNKNOWN);
				ImportHandler handler = new ImportHandler(monitor);
				handler.run(pi);
			});
			return true;
		} catch (Exception e) {
			log.error("EcoSpold 02 import failed", e);
			return false;
		} finally {
			Database.getIndexUpdater().endTransaction();
			Navigator.refresh();
			Cache.evictAll();
		}
	}

	private EcoSpold2Import createImport() {
		File[] files = importPage.getFiles();
		IDatabase database = Database.get();
		if (files == null || files.length == 0 || database == null)
			return null;
		ImportConfig conf = new ImportConfig(database);
		if (App.runsInDevMode()) {
			conf.checkFormulas = true;
			conf.skipNullExchanges = true;
			conf.withParameterFormulas = false;
			conf.withParameters = false;
		}
		EcoSpold2Import pi = new EcoSpold2Import(conf);
		pi.setFiles(files);
		return pi;
	}

	@Override
	public void addPages() {
		String[] exts = { "zip", "spold" };
		importPage = new FileImportPage(exts, false);
		addPage(importPage);
	}

}
