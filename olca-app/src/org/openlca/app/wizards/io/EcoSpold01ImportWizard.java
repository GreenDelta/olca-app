package org.openlca.app.wizards.io;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.UI;
import org.openlca.io.EcoSpoldUnitFetch;
import org.openlca.io.UnitMappingSync;
import org.openlca.io.ecospold1.input.EcoSpold01Import;
import org.openlca.io.ecospold1.input.ImportConfig;
import org.openlca.io.maps.FlowMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EcoSpold01ImportWizard extends Wizard implements IImportWizard {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	private FileImportPage filePage;
	private UnitMappingPage mappingPage;

	/**
	 * Only set if this wizard dialog was opened with an initially selected file.
	 */
	private File initialFile;

	/**
	 * Opens the ES1 import wizard for the given *.xml or *.zip file that contains
	 * EcoSpold 1 data sets.
	 */
	public static void of(File file) {
		if (file == null)
			return;
		var wiz = PlatformUI.getWorkbench()
				.getImportWizardRegistry()
				.findWizard("wizard.import.ecospold1");
		if (wiz == null) {
			ErrorReporter.on("Failed to get EcoSpold 1 import wizard");
			return;
		}
		try {
			var wizard = (EcoSpold01ImportWizard) wiz.createWizard();
			wizard.initialFile = file;
			var dialog = new WizardDialog(UI.shell(), wizard);
			dialog.setTitle(wizard.getWindowTitle());
			dialog.open();
		} catch (Exception e) {
			ErrorReporter.on("Failed to open EcoSpold 1 import wizard");
		}
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
					ErrorReporter.on("Failed to get the units from files.", e);
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
			Database.getIndexUpdater().beginTransaction();
			getContainer().run(true, true, m -> {
				m.beginTask(M.ImportEcoSpold01DataSets,
						IProgressMonitor.UNKNOWN);
				var imp = new EcoSpold01Import(config());
				imp.setFiles(filePage.getFiles());
				var handler = new ImportHandler(m);
				try {
					handler.run(imp);
				} catch (Exception e) {
					log.error("Data set import failed", e);
				}
				m.done();
			});
			return true;
		} catch (Exception e) {
			log.error("import failed ", e);
			return false;
		} finally {
			Database.getIndexUpdater().endTransaction();
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
		if (filePage.mappingFile != null) {
			config.setFlowMap(FlowMap.fromCsv(filePage.mappingFile));
		}
		return config;
	}

}
