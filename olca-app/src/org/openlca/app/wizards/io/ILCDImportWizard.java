package org.openlca.app.wizards.io;

import java.io.File;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.AppContext;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.preferences.IoPreference;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.core.io.maps.FlowMap;
import org.openlca.ilcd.io.ZipStore;
import org.openlca.io.ilcd.input.Import;

public class ILCDImportWizard extends Wizard implements IImportWizard {

	private File initialFile;
	private Page page;

	public static void of(File file) {
		if (Database.isNoneActive()) {
			MsgBox.info(M.NoDatabaseOpened, M.NeedOpenDatabase);
			return;
		}
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
		if (zip == null)
			return false;
		try {
			doRun(zip);
			return true;
		} catch (Exception e) {
			ErrorReporter.on("ILCD import failed", e);
			return false;
		} finally {
			Navigator.refresh();
			AppContext.evictAll();
		}
	}

	private void doRun(File zip) throws Exception {
		try (var store = new ZipStore(zip)) {
			getContainer().run(true, true, monitor -> {
				var flowMap = page.flowMap != null
						? page.flowMap
						: FlowMap.empty();
				var imp = Import.of(store, Database.get(), flowMap)
						.withPreferredLanguage(IoPreference.getIlcdLanguage())
						.withAllFlows(true);
				ImportMonitor.on(monitor).run(imp);
			});
		}
	}

	private static class Page extends WizardPage {

		private File zip;
		private FlowMap flowMap;

		Page(File initial) {
			super("ILCDImportWizard.Page");
			setTitle(M.ImportILCD);
			setDescription(M.ImportZipFileWithTheIlcdDataSets);
			zip = initial;
			setPageComplete(zip != null);
		}

		@Override
		public void createControl(Composite parent) {
			var body = new Composite(parent, SWT.NONE);
			UI.gridLayout(body, 1);

			var comp = UI.composite(body);
			UI.fillHorizontal(comp);
			UI.gridLayout(comp, 3);

			FileSelector.on(file -> {
						zip = file;
						setPageComplete(true);
					})
					.withDialogTitle(M.SelectAZipFileWithIlcdDataDots)
					.withExtensions("*.zip")
					.withSelection(zip)
					.render(comp);

			MappingSelector.on(fm -> this.flowMap = fm)
					.withSelectionPattern("(?i).*ilcd*.import.*")
					.render(comp);

			setControl(body);
		}
	}

}
