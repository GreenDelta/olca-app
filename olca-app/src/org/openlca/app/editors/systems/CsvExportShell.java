package org.openlca.app.editors.systems;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
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
import org.openlca.app.components.FileSelection;
import org.openlca.app.db.Cache;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Dialog;
import org.openlca.app.util.InformationPopup;
import org.openlca.app.util.UI;
import org.openlca.core.model.ProductSystem;
import org.openlca.io.xls.CsvMatrixExport;
import org.openlca.io.xls.CsvMatrixExportData;

/**
 * The dialog for exporting product systems as matrices.
 */
public class CsvExportShell extends Shell implements SelectionListener {

	private final FormToolkit toolkit = new FormToolkit(Display.getDefault());

	private String[] columnSeparators = { ",", ";", "\t" };
	private String[] columnSeparatorNames = { "Comma (,)", "Semicolon (;)",
			"Tab (\\t)" };
	private String[] decimalSeparators = { ".", "," };
	private String[] decimalSeparatorNames = { "Point (.)", "Comma (,)" };

	private Combo pointCombo;
	private Combo columnCombo;
	private FileSelection techSelection;
	private FileSelection enviSelection;
	private CsvMatrixExportData data;

	public CsvExportShell(Shell parent, ProductSystem system) {
		super(parent, SWT.SHELL_TRIM);
		setImage(ImageType.MATRIX_ICON.get());
		setLayout(new FillLayout(SWT.HORIZONTAL));
		setText("Matrix Export");
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
		Group formatGroup = createGroup("CSV Format", composite, 2);
		pointCombo = createCombo("Decimal separator", formatGroup,
				decimalSeparatorNames);
		columnCombo = createCombo("Column separator", formatGroup,
				columnSeparatorNames);
		Group fileGroup = createGroup("Matrix Files", composite, 1);
		techSelection = new FileSelection(fileGroup, toolkit,
				"Technology Matrix");
		techSelection.setDefaultFileName("technology_matrix.csv");
		techSelection.setFilter("*.csv");
		enviSelection = new FileSelection(fileGroup, toolkit,
				"Intervention Matrix");
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
		Button okButton = toolkit.createButton(composite, "OK", SWT.NONE);
		okButton.setData("_method", "ok");
		okButton.addSelectionListener(this);
		UI.gridData(okButton, false, false).widthHint = 80;
		Button cancelButton = toolkit.createButton(composite, "Cancel",
				SWT.NONE);
		cancelButton.setData("_method", "cancel");
		cancelButton.addSelectionListener(this);
		UI.gridData(cancelButton, false, false).widthHint = 80;
	}

	@Override
	protected void checkSubclass() {
	}

	@Override
	public void dispose() {
		toolkit.dispose();
		super.dispose();
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		widgetSelected(e);
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
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
			Dialog.showError(this, "The export files must be selected.");
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
		App.run("Export matrix", new CsvMatrixExport(data), new Runnable() {
			public void run() {
				InformationPopup.show("Export finished", "Export is finished");
			}
		});
		this.dispose();
	}

	private void onCancel() {
		this.dispose();
	}

}
