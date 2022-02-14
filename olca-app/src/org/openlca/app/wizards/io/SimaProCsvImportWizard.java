package org.openlca.app.wizards.io;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.FileType;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.io.maps.FlowMap;
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
		page = new Page();
		if (initialFile != null) {
			page.files.add(initialFile);
			page.setPageComplete(true);
		}
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
			Database.getWorkspaceIdUpdater().beginTransaction();
			getContainer().run(
				true, true, monitor -> ImportMonitor.on(monitor).run(imp));
			Navigator.refresh();
			return true;
		} catch (Exception e) {
			ErrorReporter.on("SimaPro CSV import failed", e);
			return false;
		} finally {
			Database.getWorkspaceIdUpdater().endTransaction();
		}
	}

	private static class Page extends WizardPage {

		final List<File> files = new ArrayList<>();
		final AtomicBoolean createProductSystems = new AtomicBoolean(true);
		final AtomicBoolean createScenarioParameters = new AtomicBoolean(true);
		final AtomicBoolean expandImpactFactors = new AtomicBoolean(false);
		FlowMap flowMap;

		Page() {
			super("SimaProCsvImportWizard.Page");
			setTitle(M.SimaProCSVImport);
			setPageComplete(false);
		}

		@Override
		public void createControl(Composite parent) {
			var body = new Composite(parent, SWT.NONE);
			UI.gridLayout(body, 1);
			UI.formLabel(body, "Selected SimaPro CSV files:");

			var viewer = Tables.createViewer(body, M.File);
			var table = viewer.getTable();
			table.setHeaderVisible(false);
			table.setLinesVisible(false);
			Tables.bindColumnWidths(table, 1.0);
			viewer.setLabelProvider(new FileLabel());
			viewer.setInput(files);

			var addFile = Actions.create(M.Add, Icon.ADD.descriptor(), () -> {
				var file = FileChooser.openFile()
					.withExtensions("*.csv")
					.withTitle("Select a SimaPro CSV File")
					.select()
					.orElse(null);
				if (file == null)
					return;
				files.add(file);
				viewer.setInput(files);
				setPageComplete(!files.isEmpty());
			});

			var removeFile = Actions.create(M.Remove, Icon.DELETE.descriptor(), () -> {
				File file = Viewers.getFirstSelected(viewer);
				if (file == null)
					return;
				files.remove(file);
				viewer.setInput(files);
				setPageComplete(!files.isEmpty());
			});
			Actions.bind(viewer, addFile, removeFile);

			// config panel
			var comp = new Composite(body, SWT.NONE);
			UI.gridLayout(comp, 2, 10, 5);
			UI.gridData(comp, true, false);

			// flow mapping
			UI.formLabel(comp, "Flow mapping:");
			MappingFileCombo.create(comp, Database.get())
				.onSelected(flowMap -> this.flowMap = flowMap);

			// options
			UI.formLabel(comp, "Generate:");
			option(comp, "Product systems for life cycles",
				createProductSystems);
			UI.filler(comp);
			option(comp, "Parameters for waste scenarios",
				createScenarioParameters);
			UI.filler(comp);
			option(comp, "Characterization factors for sub-compartments",
				expandImpactFactors);

			setControl(body);
		}

		private void option(Composite comp, String label, AtomicBoolean state) {
			var btn = new Button(comp, SWT.CHECK);
			btn.setText(label);
			btn.setSelection(state.get());
			Controls.onSelect(btn, $ -> state.set(btn.getSelection()));
		}

		private static class FileLabel extends BaseLabelProvider implements
			ITableLabelProvider {

			@Override
			public Image getColumnImage(Object obj, int col) {
				return Images.get(FileType.CSV);
			}

			@Override
			public String getColumnText(Object obj, int col) {
				return !(obj instanceof File file)
					? null
					: file.getAbsolutePath();
			}
		}

	}


}
