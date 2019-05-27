package org.openlca.app.wizards.io;

import java.io.File;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.images.Icon;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Category;
import org.openlca.io.EcoSpoldUnitFetch;
import org.openlca.io.UnitMapping;
import org.openlca.io.UnitMappingEntry;
import org.openlca.io.UnitMappingSync;
import org.openlca.io.ecospold1.input.EcoSpold01Import;
import org.openlca.io.ecospold1.input.ImportConfig;
import org.openlca.io.maps.FlowMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EcoSpold01ImportWizard extends Wizard implements IImportWizard {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private Category category;
	private FileImportPage filePage;
	private UnitMappingPage mappingPage;

	public EcoSpold01ImportWizard() {
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		filePage = new FileImportPage("zip", "xml");
		filePage.withMultiSelection = true;
		filePage.withMappingFile = true;
		addPage(filePage);

		mappingPage = new UnitMappingPage() {
			@Override
			protected String[] checkFiles(File[] files) {
				String[] unitNames;
				EcoSpoldUnitFetch ufetch = new EcoSpoldUnitFetch();
				try {
					unitNames = ufetch.getUnits(files);
				} catch (Exception e) {
					log.error("Failed to get the units from files.", e);
					unitNames = new String[0];
				}
				return unitNames;
			}

			@Override
			protected File[] getFiles() {
				return EcoSpold01ImportWizard.this.getFiles();
			}
		};
		addPage(mappingPage);
	}

	public File[] getFiles() {
		return filePage.getFiles();
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle(M.ImportEcoSpold);
		setDefaultPageImageDescriptor(Icon.IMPORT_ZIP_WIZARD
				.descriptor());
	}

	@Override
	public boolean performFinish() {
		try {
			Database.getIndexUpdater().beginTransaction();
			getContainer().run(true, true, m -> {
				m.beginTask(M.ImportEcoSpold01DataSets,
						IProgressMonitor.UNKNOWN);
				EcoSpold01Import imp = new EcoSpold01Import(config());
				imp.setProcessCategory(category);
				imp.setFiles(filePage.getFiles());
				ImportHandler handler = new ImportHandler(m);
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
		IDatabase db = Database.get();
		ImportConfig config = new ImportConfig(db);
		List<UnitMappingEntry> units = mappingPage.getUnitMappings();
		UnitMapping umap = new UnitMappingSync(db).run(units);
		config.setUnitMapping(umap);
		if (filePage.mappingFile != null) {
			config.setFlowMap(FlowMap.fromCsv(filePage.mappingFile));
		}
		return config;

	}
}
