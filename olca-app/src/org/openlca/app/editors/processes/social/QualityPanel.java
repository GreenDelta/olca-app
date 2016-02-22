package org.openlca.app.editors.processes.social;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.util.UI;
import org.openlca.core.model.PedigreeMatrix;
import org.openlca.core.model.PedigreeMatrixRow;
import org.openlca.core.model.SocialAspect;

class QualityPanel {

	private SocialAspect aspect;
	private FormToolkit tk;
	private List<QualityCell> cells = new ArrayList<>();

	QualityPanel(SocialAspect aspect) {
		this.aspect = aspect;
	}

	void create(Composite body, FormToolkit tk) {
		this.tk = tk;
		Composite comp = tk.createComposite(body);
		UI.gridData(comp, true, true);
		UI.gridLayout(comp, 6, 2, 0);
		drawHeader(comp);
		drawContent(comp);
		initSelection();
	}

	private void initSelection() {
		if (aspect.quality == null)
			return;
		Map<PedigreeMatrixRow, Integer> m = PedigreeMatrix.fromString(aspect.quality);
		for (PedigreeMatrixRow key : m.keySet()) {
			select(key, m.get(key), false);
		}
	}

	private void drawHeader(Composite comp) {
		tk.createLabel(comp, "");
		for (int i = 1; i < 6; i++) {
			Label label = tk.createLabel(comp, Integer.toString(i));
			UI.gridData(label, false, false).horizontalAlignment = SWT.CENTER;
		}
	}

	private void drawContent(Composite comp) {
		QualityLabelData data = new QualityLabelData();
		for (PedigreeMatrixRow row : PedigreeMatrixRow.values()) {
			String rowText = data.getRowLabel(row);
			Label rowLabel = tk.createLabel(comp, rowText, SWT.WRAP);
			UI.gridData(rowLabel, false, true);
			createRowData(comp, row, data);
		}
	}

	private void createRowData(Composite comp, PedigreeMatrixRow row,
			QualityLabelData data) {
		for (int i = 1; i < 6; i++) {
			QualityCell dataCell = new QualityCell(this, row, i);
			dataCell.create(comp, tk, data);
			cells.add(dataCell);
		}
	}

	void select(PedigreeMatrixRow row, int score, boolean updateValue) {
		for (QualityCell cell : cells) {
			if (cell.row == row) {
				cell.selected = (cell.score == score);
				cell.setColor();
			}
		}
		if (updateValue)
			aspect.quality = getSelection();
	}

	private String getSelection() {
		Map<PedigreeMatrixRow, Integer> vals = new HashMap<>();
		for (QualityCell cell : cells) {
			if (cell.selected)
				vals.put(cell.row, cell.score);
		}
		return PedigreeMatrix.toString(vals);
	}
}
