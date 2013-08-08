package org.openlca.app.wizards.io;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.resources.ImageType;
import org.openlca.core.model.Category;
import org.openlca.core.model.Unit;
import org.openlca.core.model.UnitGroup;
import org.openlca.io.EcoSpoldUnitFetch;
import org.openlca.io.UnitMapping;
import org.openlca.io.UnitMappingEntry;
import org.openlca.io.ecospold1.importer.EcoSpold01Import;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Import wizard for EcoSpold 01 data sets
 * 
 */
public class EcoSpold01ImportWizard extends Wizard implements IImportWizard {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private Category category;
	private FileImportPage importPage;
	private UnitMappingPage mappingPage;

	public EcoSpold01ImportWizard() {
		super();
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		importPage = new FileImportPage(new String[] { "zip", "xml" }, true);
		addPage(importPage);

		mappingPage = new UnitMappingPage() {

			@Override
			protected String[] checkFiles(final File[] files) {
				String[] unitNames;
				final EcoSpoldUnitFetch unitChecker = new EcoSpoldUnitFetch();
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

	@Override
	public void dispose() {
		super.dispose();
		if (importPage != null)
			importPage.dispose();
		if (mappingPage != null)
			mappingPage.dispose();
	}

	public File[] getFiles() {
		return importPage.getFiles();
	}

	@Override
	public void init(final IWorkbench workbench,
			final IStructuredSelection selection) {
		setWindowTitle(Messages.EcoSpoldImportWizard_WindowTitle);
		setDefaultPageImageDescriptor(ImageType.IMPORT_ZIP_WIZARD
				.getDescriptor());
	}

	@Override
	public boolean performFinish() {
		// TODO: select a category for processes
		try {
			getContainer().run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(final IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					File[] files = importPage.getFiles();
					UnitMapping unitMapping = mappingPage.getUnitMapping();
					saveUnits(unitMapping);
					parse(monitor, files, unitMapping);
				}
			});
			Navigator.refresh();
			return true;
		} catch (Exception e) {
			log.error("import failed ", e);
			return false;
		}
	}

	private void parse(IProgressMonitor monitor, File[] files,
			UnitMapping unitMapping) {
		// TODO: we should pass the monitor to the exporter
		monitor.beginTask("Import EcoSpold 01 data sets",
				IProgressMonitor.UNKNOWN);
		EcoSpold01Import importer = new EcoSpold01Import(Database.get(),
				unitMapping);
		importer.setProcessCategory(category);
		try {
			importer.run(files);
		} catch (Exception e) {
			log.error("Data set import failed", e);
		}
		monitor.done();
	}

	private void saveUnits(UnitMapping unitMapping) {
		for (String unitName : unitMapping.getUnits()) {
			UnitMappingEntry entry = unitMapping.getEntry(unitName);
			UnitGroup unitGroup = entry.getUnitGroup();
			if (unitGroup.getUnit(unitName) == null) {
				Unit unit = new Unit();
				unit.setName(unitName);
				unit.setRefId(UUID.randomUUID().toString());
				unit.setConversionFactor(unitMapping
						.getConversionFactor(unitName));
				unitGroup.getUnits().add(unit);
				try {
					Database.createDao(UnitGroup.class).update(unitGroup);
				} catch (Exception e) {
					log.error("Update unit group failed", e);
				}
			}
		}
	}
}
