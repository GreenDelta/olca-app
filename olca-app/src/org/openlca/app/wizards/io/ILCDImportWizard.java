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
import org.openlca.app.preferencepages.IoPreference;
import org.openlca.app.rcp.images.Icon;
import org.openlca.ilcd.commons.LangConfig;
import org.openlca.io.ilcd.ILCDImport;
import org.openlca.io.ilcd.input.ImportConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ILCDImportWizard extends Wizard implements IImportWizard {

	private FileImportPage importPage;

	public ILCDImportWizard() {
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		importPage = new FileImportPage(new String[] { "zip" }, false);
		addPage(importPage);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle(M.ImportILCD);
		setDefaultPageImageDescriptor(Icon.IMPORT_ZIP_WIZARD
				.descriptor());
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
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("ILCD import failed", e);
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
		getContainer().run(true, true, monitor -> {
			monitor.beginTask(M.Import, IProgressMonitor.UNKNOWN);
			ImportHandler handler = new ImportHandler(monitor);
			ILCDImport iImport = new ILCDImport(createConfig(zip));
			handler.run(iImport);
		});
	}

	private ImportConfig createConfig(File zip) {
		ImportConfig config = new ImportConfig(zip, Database.get());
		config.importFlows = true;
		config.langConfig = new LangConfig(IoPreference.getIlcdLanguage());
		return config;
	}
}
