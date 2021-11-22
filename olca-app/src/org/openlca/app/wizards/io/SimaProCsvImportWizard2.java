package org.openlca.app.wizards.io;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.Tables;

public class SimaProCsvImportWizard2 extends Wizard implements IImportWizard {

	private Page page;
	private File initialFile;

	public SimaProCsvImportWizard2() {
		setNeedsProgressMonitor(true);
		setWindowTitle(M.SimaProCSVImport);
		setDefaultPageImageDescriptor(Icon.IMPORT_ZIP_WIZARD.descriptor());
	}

	public static void of(File file) {
		Wizards.forImport("wizard.import.simapro.csv",
			(SimaProCsvImportWizard2 w) -> w.initialFile = file);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
	}

	@Override
	public void addPages() {
		page = new Page();
		if (initialFile != null) {
			page.files.add(initialFile);
		}
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		return false;
	}

	private static class Page extends WizardPage {

		final List<File> files = new ArrayList<>();

		Page() {
			super("SimaProCsvImportWizard2.Page");
			setTitle(M.SimaProCSVImport);
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

			var comp = new Composite(body, SWT.NONE);
			UI.gridLayout(comp, 3, 10, 5);
			UI.gridData(comp, true, false);
			UI.formLabel(comp, "Flow mapping:");
			var combo = new Combo(comp, SWT.READ_ONLY);
			UI.gridData(combo, true, false);
			UI.filler(comp);

			UI.formLabel(comp, "Import options:");
			new Button(comp, SWT.CHECK)
				.setText("Generate product systems for lify cycles");
			UI.filler(comp);
			UI.filler(comp);
			new Button(comp, SWT.CHECK)
			.setText("Generate parameters for waste scenarions");
			UI.filler(comp);
			UI.filler(comp);
			new Button(comp, SWT.CHECK)
			.setText("Generate characterization factors for missing sub-compartments");

			setControl(body);
		}
	}


}
