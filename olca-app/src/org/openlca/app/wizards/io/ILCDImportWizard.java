package org.openlca.app.wizards.io;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.preferences.IoPreference;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.ErrorReporter;
import org.openlca.io.ilcd.ILCDImport;
import org.openlca.io.ilcd.input.ImportConfig;

public class ILCDImportWizard extends Wizard implements IImportWizard {

	private FileImportPage importPage;

	private File initialFile;

	public static void of(File file) {
		Wizards.forImport(
				"wizard.import.ilcd",
				(ILCDImportWizard w) -> w.initialFile = file);
	}

	public ILCDImportWizard() {
		setWindowTitle(M.ImportILCD);
		setNeedsProgressMonitor(true);
		setDefaultPageImageDescriptor(
				Icon.IMPORT_ZIP_WIZARD.descriptor());
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
	}

	@Override
	public void addPages() {
		importPage = initialFile != null
				? new FileImportPage(initialFile)
				: new FileImportPage(".zip");
		importPage.withMappingFile = true;
		addPage(importPage);
	}

	@Override
	public boolean performFinish() {
		File zip = getZip();
		if (zip == null)
			return false;
		try {
			Database.getIndexUpdater().beginTransaction();
			doRun(zip);
			return true;
		} catch (Exception e) {
			ErrorReporter.on("ILCD import failed", e);
			return false;
		} finally {
			Database.getIndexUpdater().endTransaction();
			Navigator.refresh();
			Cache.evictAll();
		}
	}

	private File getZip() {
		File[] files = importPage.getFiles();
		if (files.length > 0)
			return files[0];
		return null;
	}

	private void doRun(File zip) throws Exception {
		getContainer().run(true, true, m -> {
			m.beginTask(M.Import, IProgressMonitor.UNKNOWN);
			ImportHandler handler = new ImportHandler(m);
			ILCDImport iImport = new ILCDImport(config(zip));
			handler.run(iImport);
		});
	}

	private ImportConfig config(File zip) {
		ImportConfig config = new ImportConfig(zip, Database.get());
		config.importFlows = true;
		String lang = IoPreference.getIlcdLanguage();
		if (!"en".equals(lang)) {
			config.langs = new String[]{lang, "en"};
		}
		if (importPage.flowMap != null) {
			config.setFlowMap(importPage.flowMap);
		}
		return config;
	}
}
