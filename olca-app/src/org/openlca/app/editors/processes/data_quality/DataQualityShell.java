package org.openlca.app.editors.processes.data_quality;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.M;
import org.openlca.app.util.Colors;
import org.openlca.app.util.UI;
import org.openlca.core.model.DQIndicator;
import org.openlca.core.model.DQScore;
import org.openlca.core.model.DQSystem;

public class DataQualityShell extends Shell {

	private final FormToolkit toolkit = new FormToolkit(Display.getDefault());
	private List<DataQualityCell> dataCells = new ArrayList<>();
	private final DQSystem system;
	private final String dqEntry;
	private final Double baseUncertainty;
	private final Consumer<DataQualityShell> onOk;
	private final Consumer<DataQualityShell> onDelete;
	private Text baseUncertaintyText;
	private Text valueLabel;

	public static DataQualityShell withoutUncertainty(Shell parent, DQSystem system, String dqEntry,
			Consumer<DataQualityShell> onOk,
			Consumer<DataQualityShell> onDelete) {
		return new DataQualityShell(parent, system, dqEntry, null, onOk, onDelete, false);
	}

	public static DataQualityShell withUncertainty(Shell parent, DQSystem system, String dqEntry,
			Double baseUncertainty,
			Consumer<DataQualityShell> onOk,
			Consumer<DataQualityShell> onDelete) {
		return new DataQualityShell(parent, system, dqEntry, baseUncertainty, onOk, onDelete, system.hasUncertainties);
	}

	private DataQualityShell(Shell parent, DQSystem system, String dqEntry, Double baseUncertainty,
			Consumer<DataQualityShell> onOk,
			Consumer<DataQualityShell> onDelete, boolean withUncertainty) {
		super(parent, SWT.TITLE | SWT.RESIZE | SWT.CLOSE | SWT.APPLICATION_MODAL);
		this.system = system;
		this.dqEntry = dqEntry;
		this.baseUncertainty = baseUncertainty;
		this.onOk = onOk;
		this.onDelete = onDelete;
		setLayout(new FillLayout(SWT.HORIZONTAL));
		create(withUncertainty);
		setText(M.PedigreeMatrix);
		setSize(830, 700);
		// pack();
		initSelection();
		UI.center(parent, this);
	}

	private void initSelection() {
		if (dqEntry == null)
			return;
		int[] values = system.toValues(dqEntry);
		if (values == null)
			return;
		for (int i = 1; i <= values.length; i++) {
			if (values[i - 1] == 0)
				continue;
			DQIndicator indicator = system.getIndicator(i);
			if (indicator == null)
				continue;
			DQScore score = indicator.getScore(values[i - 1]);
			if (score == null)
				continue;
			select(indicator, score);
		}
		if (baseUncertainty != null && system.hasUncertainties)
			baseUncertaintyText.setText(Double.toString(baseUncertainty));
	}

	public String getSelection() {
		boolean anySelected = false;
		int[] values = new int[system.indicators.size()];
		for (DQIndicator indicator : system.indicators) {
			DQScore selection = getSelection(indicator);
			int value = 0;
			if (selection != null) {
				value = selection.position;
				anySelected = true;
			}
			values[indicator.position - 1] = value;
		}
		if (!anySelected)
			return null;
		return system.toString(values);
	}

	private DQScore getSelection(DQIndicator indicator) {
		for (DataQualityCell cell : dataCells)
			if (cell.getIndicator() == indicator && cell.isSelected())
				return cell.getScore();
		return null;
	}

	private void create(boolean withUncertainty) {
		ScrolledForm form = toolkit.createScrolledForm(this);
		Composite root = form.getBody();
		UI.gridLayout(root, 1);
		createHeader(root);
		createSeparator(root);
		createContent(root);
		createSeparator(root);
		createFooter(root, withUncertainty);
	}

	private void createContent(Composite root) {
		Composite composite = toolkit.createComposite(root);
		composite.setLayout(new GridLayout(system.getScoreCount() + 1, false));
		UI.gridData(composite, true, true);
		createContentHeader(composite);
		createContentData(composite);
	}

	private void createContentHeader(Composite composite) {
		toolkit.createLabel(composite, "");
		for (int i = 1; i <= system.getScoreCount(); i++) {
			Label label = toolkit.createLabel(composite, system.getScoreLabel(i));
			label.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
		}
	}

	private void createContentData(Composite composite) {
		Collections.sort(system.indicators);
		for (DQIndicator indicator : system.indicators) {
			String rowText = indicator.name;
			Label rowLabel = toolkit.createLabel(composite, rowText, SWT.WRAP);
			GridData gridData = UI.gridData(rowLabel, false, true);
			gridData.widthHint = 120;
			gridData.minimumWidth = 120;
			createRowData(composite, indicator);
		}
	}

	private void createRowData(Composite composite, DQIndicator indicator) {
		Collections.sort(indicator.scores);
		for (DQScore score : indicator.scores) {
			DataQualityCell dataCell = new DataQualityCell(this, indicator, score);
			dataCell.createComponents(composite, toolkit);
			dataCells.add(dataCell);
		}
	}

	private void createHeader(Composite root) {
		Composite header = toolkit.createComposite(root);
		header.setLayout(new GridLayout(1, false));
		UI.gridData(header, true, false);
		toolkit.paintBordersFor(header);
		toolkit.createLabel(header,
				M.PedigreeMatrixMessage);
	}

	private void createSeparator(Composite root) {
		Composite sep = toolkit.createCompositeSeparator(root);
		UI.gridData(sep, true, false).heightHint = 1;
		toolkit.paintBordersFor(sep);
	}

	private void createFooter(Composite root, boolean withUncertainty) {
		Composite composite = toolkit.createComposite(root);
		UI.gridLayout(composite, withUncertainty ? 7 : 3);
		GridData gd = UI.gridData(composite, true, false);
		gd.horizontalAlignment = SWT.END;
		toolkit.paintBordersFor(composite);
		if (withUncertainty) {
			toolkit.createLabel(composite, M.BaseUncertainty + ": ");
			baseUncertaintyText = toolkit.createText(composite, "1.0");
			UI.gridData(baseUncertaintyText, false, false).widthHint = 80;
			baseUncertaintyText.addModifyListener((e) -> calculateSigmaG());
			toolkit.createLabel(composite, "\u03c3g: ");
			valueLabel = toolkit.createText(composite, "", SWT.NONE);
			valueLabel.setEditable(false);
			UI.gridData(valueLabel, true, false);
		}
		createButtons(composite);
	}

	private void createButtons(Composite composite) {
		Button okBtn = toolkit.createButton(composite, M.OK, SWT.NONE);
		UI.gridData(okBtn, false, false).widthHint = 60;
		okBtn.addSelectionListener(new DataQualityFinishHandler(this, onOk));
		if (dqEntry != null) {
			Button deleteBtn = toolkit.createButton(composite, M.Delete,
					SWT.NONE);
			UI.gridData(deleteBtn, false, false).widthHint = 60;
			deleteBtn.addSelectionListener(new DataQualityFinishHandler(this, onDelete));
		}
		Button cancelBtn = toolkit.createButton(composite, M.Cancel,
				SWT.NONE);
		cancelBtn.addSelectionListener(new DataQualityFinishHandler(this, null));
		UI.gridData(cancelBtn, false, false).widthHint = 60;
	}

	void calculateSigmaG() {
		if (baseUncertaintyText == null)
			return;
		String baseFactorText = baseUncertaintyText.getText();
		try {
			double baseFactor = Double.parseDouble(baseFactorText);
			double sigma = calculateGeometricSD(baseFactor);
			valueLabel.setText(Double.toString(sigma));
			baseUncertaintyText.setBackground(getDisplay().getSystemColor(
					SWT.COLOR_WHITE));
			baseUncertaintyText.setToolTipText(null);
		} catch (Exception e) {
			baseUncertaintyText.setBackground(Colors.errorColor());
			baseUncertaintyText.setToolTipText(M.InvalidValue);
		}
	}

	private double calculateGeometricSD(double baseFactor) {
		double varSum = 0;
		for (DQIndicator indicator : system.indicators) {
			DQScore selectedScore = getSelection(indicator);
			if (selectedScore == null)
				selectedScore = indicator.scores.get(indicator.scores.size() - 1);
			double factor = selectedScore.uncertainty;
			varSum += Math.pow(Math.log(factor), 2);
		}
		varSum += Math.pow(Math.log(baseUncertainty), 2);
		return Math.sqrt(Math.exp(Math.sqrt(varSum)));
	}

	public double getBaseValue() {
		String baseFactorText = baseUncertaintyText.getText();
		try {
			return Double.parseDouble(baseFactorText);
		} catch (Exception e) {
			return Double.NaN;
		}
	}

	@Override
	protected void checkSubclass() {
	}

	void select(DQIndicator indicator, DQScore score) {
		for (DataQualityCell cell : dataCells) {
			if (cell.getIndicator() == indicator) {
				boolean selected = cell.getScore() == score;
				if (selected && cell.isSelected())
					selected = false;
				cell.setSelected(selected);
				cell.setColor();
			}
		}
	}

}
