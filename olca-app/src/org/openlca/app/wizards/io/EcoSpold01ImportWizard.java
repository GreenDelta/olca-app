package org.openlca.app.wizards.io;

import java.io.File;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.Messages;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.ImageType;
import org.openlca.core.model.Category;
import org.openlca.io.EcoSpoldUnitFetch;
import org.openlca.io.UnitMapping;
import org.openlca.io.UnitMappingEntry;
import org.openlca.io.UnitMappingSync;
import org.openlca.io.ecospold1.input.EcoSpold01Import;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EcoSpold01ImportWizard extends Wizard implements IImportWizard {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private Category category;
	private FileImportPage importPage;
	private UnitMappingPage mappingPage;

	public EcoSpold01ImportWizard() {
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		importPage = new FileImportPage(new String[] { "zip", "xml" }, true);
		addPage(importPage);

		mappingPage = new UnitMappingPage() {
			@Override
			protected String[] checkFiles(File[] files) {
				String[] unitNames;
				EcoSpoldUnitFetch unitChecker = new EcoSpoldUnitFetch();
				try {
					unitNames = unitChecker.getUnits(files);
				} catch (Exception e) {
					log.error("Failed to get the units from files.");
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
		return importPage.getFiles();
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle(Messages.ImportEcoSpold);
		setDefaultPageImageDescriptor(ImageType.IMPORT_ZIP_WIZARD
				.getDescriptor());
	}

	@Override
	public boolean performFinish() {
		try {
			getContainer().run(true, true, (monitor) -> {
				File[] files = importPage.getFiles();
				List<UnitMappingEntry> mappings = mappingPage
						.getUnitMappings();
				UnitMapping mapping = new UnitMappingSync(Database.get())
						.run(mappings);
				parse(monitor, files, mapping);
			});
			return true;
		} catch (Exception e) {
			log.error("import failed ", e);
			return false;
		} finally {
			Navigator.refresh();
			Cache.evictAll();
		}
	}

	private void parse(IProgressMonitor monitor, File[] files,
			UnitMapping unitMapping) {
		monitor.beginTask(Messages.ImportEcoSpold01DataSets,
				IProgressMonitor.UNKNOWN);
		EcoSpold01Import importer = new EcoSpold01Import(Database.get(),
				unitMapping);
		importer.setProcessCategory(category);
		importer.setFiles(files);
		ImportHandler handler = new ImportHandler(monitor);
		try {
			handler.run(importer);
		} catch (Exception e) {
			log.error("Data set import failed", e);
		}
		monitor.done();
	}

}
