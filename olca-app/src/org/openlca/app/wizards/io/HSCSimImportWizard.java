package org.openlca.app.wizards.io;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.core.io.maps.FlowMap;
import org.openlca.io.HSCSim;

/**
 * A wizard for importing HSC Sim flow sheets as process data sets.
 */
public class HSCSimImportWizard extends Wizard implements IImportWizard {

	private Page page;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("Import an HSC Sim Flow Sheet");
		setDefaultPageImageDescriptor(Icon.IMPORT_WIZARD.descriptor());
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		if (Database.isNoneActive()) {
			addPage(new NoDatabaseErrorPage());
			return;
		}
		page = new Page();
		addPage(page);
	}

	@Override
	public boolean performFinish() {

		// check that we have an open database
		var db = Database.get();
		if (db == null) {
			MsgBox.error(M.NoDatabaseOpened, M.NeedOpenDatabase);
			return false;
		}

		// we currently only support single file imports
		var file =page.json;
		if (file == null)
			return false;

		// run the import
		try {
			getContainer().run(true, true, m -> {
				m.beginTask(
						"Import " + file.getName(),
						IProgressMonitor.UNKNOWN);
				var flowMap = page.flowMap != null
						? page.flowMap
						: FlowMap.empty();
				HSCSim.importProcess(db, file, flowMap);
				m.done();
			});
			Navigator.refresh();
			return true;
		} catch (Exception e) {
			ErrorReporter.on("HSC SIM import failed; file: " + file.getPath(), e);
			return false;
		}
	}

	private static class Page extends WizardPage {

		private File json;
		private FlowMap flowMap;

		Page() {
			super("ILCDImportWizard.Page");
			setTitle("Import an HSC Sim Flow Sheet");
			setDescription("Select a *.json file with the flow sheet");
			setPageComplete(false);
		}

		@Override
		public void createControl(Composite parent) {
			var body = new Composite(parent, SWT.NONE);
			UI.gridLayout(body, 1);

			var comp = UI.composite(body);
			UI.fillHorizontal(comp);
			UI.gridLayout(comp, 3);

			FileSelector.on(file -> {
						json = file;
						setPageComplete(true);
					})
					.withTitle("Select a *.json file with the flow sheet")
					.withExtensions("*.json")
					.withSelection(json)
					.render(comp);

			MappingSelector.on(fm -> this.flowMap = fm)
					.render(comp);
			setControl(body);
		}

	}
}
