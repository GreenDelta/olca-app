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
import org.openlca.app.Messages;
import org.openlca.app.components.FileSelection;
import org.openlca.app.db.Cache;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Dialog;
import org.openlca.app.util.InformationPopup;
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
			Messages.Comma + " (,)",
			Messages.Semicolon + " (;)",
			Messages.Tab + " (\\t)" };
	private String[] decimalSeparators = { ".", "," };
	private String[] decimalSeparatorNames = {
			Messages.Dot + " (.)",
			Messages.Comma + " (,)" };

	private Combo pointCombo;
	private Combo columnCombo;
	private FileSelection techSelection;
	private FileSelection enviSelection;
	private CsvMatrixExportData data;

	public CsvExportShell(Shell parent, ProductSystem system) {
		super(parent, SWT.SHELL_TRIM);
		setImage(ImageType.MATRIX_ICON.get());
		setLayout(new FillLayout(SWT.HORIZONTAL));
		setText(Messages.MatrixExport);
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
		Group formatGroup = createGroup(Messages.CSVFormat, composite, 2);
		pointCombo = createCombo(Messages.DecimalSeparator, formatGroup,
				decimalSeparatorNames);
		columnCombo = createCombo(Messages.ColumnSeparator, formatGroup,
				columnSeparatorNames);
		Group fileGroup = createGroup(Messages.MatrixFiles, composite, 1);
		techSelection = new FileSelection(fileGroup, toolkit,
				Messages.TexchnologyMatrix);
		techSelection.setDefaultFileName("technology_matrix.csv");
		techSelection.setFilter("*.csv");
		enviSelection = new FileSelection(fileGroup, toolkit,
				Messages.InterventionMatrix);
		enviSelection.setDefaultFileName("intervention_matrix.csv");
		enviSelection.setFilter("*.csv");
		createButtons(composite);
	}

	private Group createGroup(String label, Composite parent, int cols) {
		Group group = new Group(parent, SWT.NONE);
		UI.gridLayout(group, cols);
		UI.gridData(group, true, false);
		group.setText(label);
		UI.adapt(toolkit, group);
		return group;
	}

	private Combo createCombo(String label, Composite parent, String[] options) {
		toolkit.createLabel(parent, label, SWT.NONE);
		Combo combo = new Combo(parent, SWT.NONE);
		UI.gridData(combo, false, false).widthHint = 150;
		UI.adapt(toolkit, combo);
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
		Button ok = toolkit.createButton(composite, Messages.OK, SWT.NONE);
		ok.setData("_method", "ok");
		Controls.onSelect(ok, (e) -> buttonPressed(e));
		UI.gridData(ok, false, false).widthHint = 80;
		Button cancel = toolkit.createButton(composite, Messages.Cancel,
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
			Dialog.showError(this, Messages.NoExportFilesSelected);
			return;
		}
		data.setInterventionFile(enviFile);
		data.setTechnologyFile(techFile);
		int idx = pointCombo.getSelectionIndex();
		String point = idx >= 0 ? decimalSeparators[idx] : pointCombo.getText();
		data.setDecimalSeparator(point);
		idx = columnCombo.getSelectionIndex();
		String column = idx >= 0 ? columnSeparators[idx] : columnCombo
				.getText();
		data.setColumnSeperator(column);
		App.run(Messages.ExportMatrix,
				new CsvMatrixExport(data),
				() -> InformationPopup.show(Messages.ExportDone));
		this.dispose();
	}

	private void onCancel() {
		this.dispose();
	}

}
