package org.openlca.app.editors.systems;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.FileSelection;
import org.openlca.app.db.Cache;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Dialog;
import org.openlca.app.util.FileType;
import org.openlca.app.util.Info;
import org.openlca.app.util.UI;
import org.openlca.core.model.ProductSystem;
import org.openlca.io.xls.CsvMatrixExport;
import org.openlca.io.xls.CsvMatrixExportData;

/**
 * The dialog for exporting product systems as matrices.
 */
public class CsvExportShell extends Shell {

	private final FormToolkit toolkit = new FormToolkit(Display.getDefault());

	private String[] columnSeparators = { ",", ";", "\t" };
	private String[] columnSeparatorNames = {
			M.Comma + " (,)",
			M.Semicolon + " (;)",
			M.Tab + " (\\t)" };
	private String[] decimalSeparators = { ".", "," };
	private String[] decimalSeparatorNames = {
			M.Dot + " (.)",
			M.Comma + " (,)" };

	private Combo pointCombo;
	private Combo columnCombo;
	private FileSelection techSelection;
	private FileSelection enviSelection;
	private CsvMatrixExportData data;

	public CsvExportShell(Shell parent, ProductSystem system) {
		super(parent, SWT.SHELL_TRIM);
		setImage(Images.get(FileType.CSV));
		setLayout(new FillLayout(SWT.HORIZONTAL));
		setText(M.MatrixExport);
		setSize(450, 450);
		createContents();
		data = new CsvMatrixExportData();
		data.setMatrixCache(Cache.getMatrixCache());
		data.setEntityCache(Cache.getEntityCache());
		data.setProductSystem(system);
		UI.center(parent, this);
	}

	protected void createContents() {
		Composite composite = toolkit.createComposite(this, SWT.NONE);
		toolkit.paintBordersFor(composite);
		UI.gridLayout(composite, 1);
		Group formatGroup = createGroup(M.CSVFormat, composite, 2);
		pointCombo = createCombo(M.DecimalSeparator, formatGroup,
				decimalSeparatorNames);
		columnCombo = createCombo(M.ColumnSeparator, formatGroup,
				columnSeparatorNames);
		Group fileGroup = createGroup(M.MatrixFiles, composite, 1);
		techSelection = new FileSelection(fileGroup, toolkit,
				M.TexchnologyMatrix);
		techSelection.setDefaultFileName("technology_matrix.csv");
		techSelection.setFilter("*.csv");
		enviSelection = new FileSelection(fileGroup, toolkit,
				M.InterventionMatrix);
		enviSelection.setDefaultFileName("intervention_matrix.csv");
		enviSelection.setFilter("*.csv");
		createButtons(composite);
	}

	private Group createGroup(String label, Composite parent, int cols) {
		Group group = new Group(parent, SWT.NONE);
		UI.gridLayout(group, cols);
		UI.gridData(group, true, false);
		group.setText(label);
		toolkit.adapt(group);
		toolkit.paintBordersFor(group);
		return group;
	}

	private Combo createCombo(String label, Composite parent, String[] options) {
		toolkit.createLabel(parent, label, SWT.NONE);
		Combo combo = new Combo(parent, SWT.NONE);
		UI.gridData(combo, false, false).widthHint = 150;
		toolkit.adapt(combo);
		toolkit.paintBordersFor(combo);
		combo.setItems(options);
		combo.select(0);
		return combo;
	}

	private void createButtons(Composite parent) {
		Composite composite = toolkit.createComposite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false,
				false));
		toolkit.paintBordersFor(composite);
		UI.gridLayout(composite, 2);
		Button ok = toolkit.createButton(composite, M.OK, SWT.NONE);
		ok.setData("_method", "ok");
		Controls.onSelect(ok, (e) -> buttonPressed(e));
		UI.gridData(ok, false, false).widthHint = 80;
		Button cancel = toolkit.createButton(composite, M.Cancel,
				SWT.NONE);
		cancel.setData("_method", "cancel");
		Controls.onSelect(cancel, (e) -> buttonPressed(e));
		UI.gridData(cancel, false, false).widthHint = 80;
	}

	@Override
	protected void checkSubclass() {
	}

	@Override
	public void dispose() {
		toolkit.dispose();
		super.dispose();
	}

	private void buttonPressed(SelectionEvent e) {
		Object method = e.widget.getData("_method");
		if (method == null)
			return;
		if (method.equals("ok"))
			onOk();
		else if (method.equals("cancel"))
			onCancel();
	}

	private void onOk() {
		File enviFile = enviSelection.getFile();
		File techFile = techSelection.getFile();
		if (enviFile == null || techFile == null) {
			Dialog.showError(this, M.NoExportFilesSelected);
			return;
		}
		data.setInterventionFile(enviFile);
		data.setTechnologyFile(techFile);
		int idx = pointCombo.getSelectionIndex();
		String point = idx >= 0 ? decimalSeparators[idx] : pointCombo.getText();
		data.setDecimalSeparator(point);
		idx = columnCombo.getSelectionIndex();
		String column = idx >= 0 ? columnSeparators[idx]
				: columnCombo
						.getText();
		data.setColumnSeperator(column);
		App.run(M.ExportMatrix,
				new CsvMatrixExport(data),
				() -> Info.popup(M.ExportDone));
		this.dispose();
	}

	private void onCancel() {
		this.dispose();
	}

}
