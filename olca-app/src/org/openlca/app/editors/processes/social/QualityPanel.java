package org.openlca.app.editors.processes.social;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.util.UI;
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
			GridData gridData = UI.gridData(rowLabel, false, true);
			// gridData.widthHint = 50;
			// gridData.minimumWidth = 50;
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

	void select(PedigreeMatrixRow row, int score) {
		for (QualityCell cell : cells) {
			if (cell.row == row) {
				cell.selected = (cell.score == score);
				cell.setColor();
			}
		}
	}
}
