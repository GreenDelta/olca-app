package org.openlca.app.wizards.io;

import java.io.File;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.core.io.maps.FlowMap;
import org.openlca.io.UnitMappingSync;
import org.openlca.io.ecospold1.input.ES1UnitFetch;
import org.openlca.io.ecospold1.input.EcoSpold01Import;
import org.openlca.io.ecospold1.input.ImportConfig;

public class EcoSpold01ImportWizard extends Wizard implements IImportWizard {

	private FilePage filePage;
	private UnitMappingPage mappingPage;

	private File initialFile;

	public static void of(File file) {
		if (Database.isNoneActive()) {
			MsgBox.error(M.NoDatabaseOpened, M.NeedOpenDatabase);
			return;
		}
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
		if (Database.isNoneActive()) {
			addPage(new NoDatabaseErrorPage());
			return;
		}

		filePage = new FilePage(initialFile);
		addPage(filePage);

		mappingPage = new UnitMappingPage() {
			@Override
			protected String[] checkFiles(File[] files) {
				try {
					return new ES1UnitFetch().getUnits(files);
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
						: filePage.files();
			}
		};
		addPage(mappingPage);
	}

	@Override
	public boolean performFinish() {
		try {
			getContainer().run(true, true, m -> {
				var imp = new EcoSpold01Import(config());
				imp.setFiles(filePage.files());
				ImportMonitor.on(m).run(imp);
			});
			return true;
		} catch (Exception e) {
			ErrorReporter.on("EcoSpold 1 import failed", e);
			return false;
		} finally {
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

	private static class FilePage extends WizardPage {

		private List<File> _files;
		FlowMap flowMap;

		FilePage(File initial) {
			super("EcoSpold01ImportWizard.FilePage");
			setTitle("Import EcoSpold 1 data sets");
			setDescription("Import data sets from Xml or Zip files");
			this._files = initial != null
				? List.of(initial)
				: List.of();
			setPageComplete(!_files.isEmpty());
		}

		File[] files() {
			return _files.toArray(File[]::new);
		}

		@Override
		public void createControl(Composite parent) {
			var body = UI.composite(parent);
			UI.gridLayout(body, 1);

			FilePanel.on(files -> {
						this._files = files;
						setPageComplete(!files.isEmpty());
					})
					.withExtensions("*.xml", "*.zip")
					.withFiles(_files)
					.render(body);

			var mapComp = UI.composite(body);
			UI.gridLayout(mapComp, 3);
			UI.fillHorizontal(mapComp);
			MappingSelector.on(fm -> this.flowMap = fm)
					.withSelectionPattern("(?i).*ecospold*.1*.import.*")
					.render(mapComp);

			setControl(body);
		}
	}
}
