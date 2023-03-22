package org.openlca.app.wizards.io;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.jsonld.ZipStore;
import org.openlca.jsonld.input.JsonImport;
import org.openlca.jsonld.input.UpdateMode;
import org.slf4j.LoggerFactory;

public class JsonImportWizard extends Wizard implements IImportWizard {

	private Page page;
	private File initialFile;

	public static void of(File file) {
		if (Database.isNoneActive()) {
			MsgBox.info(M.NoDatabaseOpened, M.NeedOpenDatabase);
			return;
		}
		Wizards.forImport(
				"wizard.import.json",
				(JsonImportWizard w) -> w.initialFile = file);
	}

	public JsonImportWizard() {
		setNeedsProgressMonitor(true);
		setWindowTitle("openLCA JSON-LD Import");
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
		var zip = page.zip;
		if (zip == null || !zip.exists())
			return false;
		try {
			doRun(zip);
			return true;
		} catch (Exception e) {
			ErrorReporter.on("JSON import failed", e);
			return false;
		} finally {
			Navigator.refresh();
			Cache.evictAll();
		}
	}

	private void doRun(File zip) throws Exception {
		var mode = page.updateMode;
		LoggerFactory.getLogger(getClass())
				.info("Import JSON LD package {} with update mode = {}", zip, mode);
		getContainer().run(true, true, (monitor) -> {
			monitor.beginTask(M.Import, IProgressMonitor.UNKNOWN);
			try (var store = ZipStore.open(zip)) {
				var importer = new JsonImport(store, Database.get());
				importer.setUpdateMode(mode);
				importer.run();
			} catch (Exception e) {
				throw new InvocationTargetException(e);
			}
		});
	}

	/**
	 * Contains settings for the JSON-LD import.
	 */
	private static class Page extends WizardPage {

		private final UpdateMode[] mods = {
				UpdateMode.NEVER,
				UpdateMode.IF_NEWER,
				UpdateMode.ALWAYS
		};
		UpdateMode updateMode = UpdateMode.NEVER;
		File zip;

		Page(File zip) {
			super("JsonImportPage");
			setTitle("Import an openLCA data package");
			this.zip = zip;
			setPageComplete(zip != null);
		}

		@Override
		public void createControl(Composite parent) {
			var body = UI.composite(parent);
			UI.gridLayout(body, 1).verticalSpacing = 0;

			var fileComp = UI.composite(body);
			UI.fillHorizontal(fileComp);
			UI.gridLayout(fileComp, 3).marginBottom = 0;
			FileSelector.on(file -> {
						zip = file;
						setPageComplete(true);
					})
					.withTitle("Select a zip file with openLCA data...")
					.withExtensions("*.zip")
					.withSelection(zip)
					.render(fileComp);

			// update mode
			var groupComp = UI.composite(body);
			UI.gridLayout(groupComp, 1).marginTop = 0;
			UI.fillHorizontal(groupComp);
			var group = UI.group(groupComp);
			group.setText("Updating existing data sets in the database");
			UI.gridData(group, true, false);
			UI.gridLayout(group, 1);
			for (UpdateMode mode : mods) {
				var option = new Button(group, SWT.RADIO);
				option.setText(getText(mode));
				option.setSelection(mode == updateMode);
				Controls.onSelect(option, (e) -> updateMode = mode);
			}
			setControl(body);
		}

		private String getText(UpdateMode mode) {
			return switch (mode) {
				case NEVER -> "Never update a data set that already exists";
				case IF_NEWER -> "Update data sets with newer versions";
				case ALWAYS -> "Overwrite all existing data sets";
			};
		}
	}
}
