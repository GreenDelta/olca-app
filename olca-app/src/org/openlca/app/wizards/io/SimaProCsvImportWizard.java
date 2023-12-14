package org.openlca.app.wizards.io;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.core.io.maps.FlowMap;
import org.openlca.io.simapro.csv.input.SimaProCsvImport;

public class SimaProCsvImportWizard extends Wizard implements IImportWizard {

	private Page page;
	private File initialFile;

	public SimaProCsvImportWizard() {
		setNeedsProgressMonitor(true);
		setWindowTitle(M.SimaProCSVImport);
		setDefaultPageImageDescriptor(Icon.SIMAPRO_WIZARD.descriptor());
	}

	public static void of(File file) {
		if (Database.isNoneActive()) {
			MsgBox.info(M.NoDatabaseOpened, M.NeedOpenDatabase);
			return;
		}
		Wizards.forImport("wizard.import.simapro.csv",
				(SimaProCsvImportWizard w) -> {
					w.initialFile = file;
					if (w.page != null) {
						w.page.files.add(file);
						w.page.setPageComplete(true);
					}
				});
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
		if (page == null || page.files.isEmpty())
			return false;
		var db = Database.get();
		if (db == null) {
			MsgBox.error("No database is opened");
			return false;
		}

		var files = page.files.toArray(File[]::new);
		var imp = new SimaProCsvImport(db, files)
				.withFlowMap(page.flowMap)
				.generateLifeCycleSystems(page.createProductSystems.get())
				.unrollWasteScenarios(page.createScenarioParameters.get())
				.expandImpactFactors(page.expandImpactFactors.get());

		try {
			getContainer().run(true, true, monitor -> ImportMonitor.on(monitor).run(imp));
			Navigator.refresh();
			return true;
		} catch (Exception e) {
			ErrorReporter.on("SimaPro CSV import failed", e);
			return false;
		}
	}

	private static class Page extends WizardPage {

		List<File> files;
		final AtomicBoolean createProductSystems = new AtomicBoolean(true);
		final AtomicBoolean createScenarioParameters = new AtomicBoolean(true);
		final AtomicBoolean expandImpactFactors = new AtomicBoolean(false);
		FlowMap flowMap;

		Page(File initial) {
			super("SimaProCsvImportWizard.Page");
			setTitle(M.SimaProCSVImport);
			this.files = initial != null
					? List.of(initial)
					: List.of();
			setPageComplete(!files.isEmpty());
		}

		@Override
		public void createControl(Composite parent) {
			var body = UI.composite(parent);
			UI.gridLayout(body, 1);

			FilePanel.on(files -> {
						this.files = files;
						setPageComplete(!files.isEmpty());
					})
					.withTitle("Selected SimaPro CSV files")
					.withExtensions("*.csv")
					.withFiles(files)
					.render(body);

			// config panel
			var mappingComp = UI.composite(body);
			UI.gridLayout(mappingComp, 3, 10, 5).marginBottom = 0;
			UI.fillHorizontal(mappingComp);
			// flow mapping
			MappingSelector.on(fm -> this.flowMap = fm)
					.withSelectionPattern("(?i).*simapro*.import.*")
					.render(mappingComp);

			// options
			var optComp = UI.composite(body);
			UI.gridLayout(optComp, 1, 10, 5).marginTop = 0;
			UI.fillHorizontal(optComp);
			var group = UI.group(optComp);
			UI.gridLayout(group, 1, 5, 10);
			UI.fillHorizontal(group);
			group.setText("Generate");
			option(group, "Product systems for life cycles",
					createProductSystems);
			option(group, "Parameters for waste scenarios",
					createScenarioParameters);
			option(group, "Characterization factors for sub-compartments",
					expandImpactFactors);

			setControl(body);
		}

		private void option(Composite comp, String label, AtomicBoolean state) {
			var btn = new Button(comp, SWT.CHECK);
			btn.setText(label);
			btn.setSelection(state.get());
			Controls.onSelect(btn, $ -> state.set(btn.getSelection()));
		}
	}
}
