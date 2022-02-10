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
import org.openlca.io.EcoSpoldUnitFetch;
import org.openlca.io.UnitMappingSync;
import org.openlca.io.ecospold1.input.EcoSpold01Import;
import org.openlca.io.ecospold1.input.ImportConfig;

public class EcoSpold01ImportWizard extends Wizard implements IImportWizard {

	private FileImportPage filePage;
	private UnitMappingPage mappingPage;

	private File initialFile;

	public static void of(File file) {
		Wizards.forImport(
				"wizard.import.ecospold1",
				(EcoSpold01ImportWizard w) -> w.initialFile = file);
	}

	public EcoSpold01ImportWizard() {
		setWindowTitle(M.ImportEcoSpold);
		setDefaultPageImageDescriptor(Icon.IMPORT_ZIP_WIZARD.descriptor());
		setNeedsProgressMonitor(true);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
	}

	@Override
	public void addPages() {
		filePage = initialFile != null
				? new FileImportPage(initialFile)
				: new FileImportPage("zip", "xml");
		filePage.withMultiSelection = true;
		filePage.withMappingFile = true;
		addPage(filePage);

		mappingPage = new UnitMappingPage() {
			@Override
			protected String[] checkFiles(File[] files) {
				try {
					return new EcoSpoldUnitFetch().getUnits(files);
				} catch (Exception e) {
					var m = "Failed to get the units from files.";
					ErrorReporter.on(m, e);
					return new String[0];
				}
			}

			@Override
			protected File[] getFiles() {
				return filePage == null
						? new File[0]
						: filePage.getFiles();
			}
		};
		addPage(mappingPage);
	}

	@Override
	public boolean performFinish() {
		try {
			Database.getWorkspaceIdUpdater().beginTransaction();
			getContainer().run(true, true, m -> {
				var imp = new EcoSpold01Import(config());
				imp.setFiles(filePage.getFiles());
				ImportMonitor.on(m).run(imp);
			});
			return true;
		} catch (Exception e) {
			ErrorReporter.on("EcoSpold 1 import failed", e);
			return false;
		} finally {
			Database.getWorkspaceIdUpdater().endTransaction();
			Navigator.refresh();
			Cache.evictAll();
		}
	}

	private ImportConfig config() {
		var db = Database.get();
		var config = new ImportConfig(db);
		var units = mappingPage.getUnitMappings();
		var umap = new UnitMappingSync(db).run(units);
		config.setUnitMapping(umap);
		if (filePage.flowMap != null) {
			config.setFlowMap(filePage.flowMap);
		}
		return config;
	}
}
