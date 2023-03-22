package org.openlca.app.wizards.io;

import java.io.File;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.core.database.IDatabase;
import org.openlca.core.io.maps.FlowMap;
import org.openlca.io.ecospold2.input.EcoSpold2Import;
import org.openlca.io.ecospold2.input.ImportConfig;

/**
 * Import wizard for files in the EcoSpold format version 2.
 */
public class EcoSpold2ImportWizard extends Wizard implements IImportWizard {

	private Page page;
	private File initialFile;

	public static void of(File file) {
		if (Database.isNoneActive()) {
			MsgBox.info(M.NoDatabaseOpened, M.NeedOpenDatabase);
			return;
		}
		Wizards.forImport(
				"wizard.import.ecospold2",
				(EcoSpold2ImportWizard w) -> w.initialFile = file);
	}

	public EcoSpold2ImportWizard() {
		setWindowTitle(M.ImportEcoSpold02DataSets);
		setNeedsProgressMonitor(true);
		setDefaultPageImageDescriptor(
				Icon.IMPORT_ZIP_WIZARD.descriptor());
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
		page = new Page(initialFile);
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		var imp = createImport();
		if (imp == null)
			return false;
		try {
			getContainer().run(true, true, monitor -> ImportMonitor.on(monitor).run(imp));
			return true;
		} catch (Exception e) {
			ErrorReporter.on("EcoSpold 02 import failed", e);
			return false;
		} finally {
			Navigator.refresh();
			Cache.evictAll();
		}
	}

	private EcoSpold2Import createImport() {
		var files = page.files();
		IDatabase db = Database.get();
		if (files == null || files.length == 0 || db == null)
			return null;
		var conf = new ImportConfig(db);
		if (App.runsInDevMode()) {
			conf.checkFormulas = true;
			conf.skipNullExchanges = true;
			conf.withParameterFormulas = false;
			conf.withParameters = false;
		}
		if (page.flowMap != null) {
			conf.setFlowMap(page.flowMap);
		}
		EcoSpold2Import pi = new EcoSpold2Import(conf);
		pi.setFiles(files);
		return pi;
	}

	private static class Page extends WizardPage {
		private List<File> _files;
		FlowMap flowMap;

		Page(File initial) {
			super("EcoSpold2ImportWizard.Page");
			setTitle("Import EcoSpold 2 data sets");
			setDescription("Import data sets from EcoSpold 2 files");
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
					.withExtensions("*.xml", "*.zip", "*.spold")
					.withFiles(_files)
					.render(body);

			var mapComp = UI.composite(body);
			UI.gridLayout(mapComp, 3);
			UI.fillHorizontal(mapComp);
			MappingSelector.on(fm -> this.flowMap = fm)
					.render(mapComp);

			setControl(body);
		}
	}
}
