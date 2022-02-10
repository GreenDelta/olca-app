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
import org.openlca.app.preferences.IoPreference;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.ErrorReporter;
import org.openlca.ilcd.io.ZipStore;
import org.openlca.io.ilcd.ILCDImport;
import org.openlca.io.ilcd.input.ImportConfig;
import org.openlca.io.maps.FlowMap;

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
			Database.getWorkspaceIdUpdater().beginTransaction();
			doRun(zip);
			return true;
		} catch (Exception e) {
			ErrorReporter.on("ILCD import failed", e);
			return false;
		} finally {
			Database.getWorkspaceIdUpdater().endTransaction();
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
		try (var store = new ZipStore(zip)) {
			getContainer().run(true, true, m -> {
				var lang = IoPreference.getIlcdLanguage();
				var langOrder = !"en".equals(lang)
					? new String[]{lang, "en"}
					: new String[]{"en"};
				var flowMap = importPage.flowMap != null
					? importPage.flowMap
					: FlowMap.empty();

				var config = new ImportConfig(store, Database.get(), flowMap)
					.withAllFlows(true)
					.withLanguageOrder(langOrder);
				ImportMonitor.on(m).run(new ILCDImport(config));
			});
		}
	}

}
