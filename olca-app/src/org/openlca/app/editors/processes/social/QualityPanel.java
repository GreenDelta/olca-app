package org.openlca.app.editors.processes.social;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.util.UI;
import org.openlca.core.model.DQIndicator;
import org.openlca.core.model.DQScore;
import org.openlca.core.model.DQSystem;
import org.openlca.core.model.SocialAspect;

class QualityPanel {

	private SocialAspect aspect;
	private DQSystem system;
	private FormToolkit tk;
	private List<QualityCell> cells = new ArrayList<>();

	QualityPanel(SocialAspect aspect, DQSystem system) {
		this.aspect = aspect;
		this.system = system;
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
		String[] values = aspect.quality.substring(1, aspect.quality.length() - 1).split(";");
		if (values == null || values.length == 0)
			return;
		for (int i = 1; i <= values.length; i++) {
			if ("n.a.".equals(values[i - 1]))
				continue;
			int value = Integer.parseInt(values[i - 1]);
			DQIndicator indicator = system.getIndicator(i);
			if (indicator == null)
				continue;
			DQScore score = indicator.getScore(value);
			if (score == null)
				continue;
			select(indicator, score, false);
		}
	}

	public String getSelection() {
		boolean anySelected = false;
		String selected = null;
		// already sorted in initSelection
		for (DQIndicator indicator : system.indicators) {
			if (selected == null) {
				selected = "(";
			} else {
				selected += ";";
			}
			DQScore selection = getSelection(indicator);
			if (selection == null) {
				selected += "n.a.";
				continue;
			}
			selected += Integer.toString(selection.position);
			anySelected = true;
		}
		if (selected == null || !anySelected)
			return null;
		return selected + ")";
	}

	private DQScore getSelection(DQIndicator indicator) {
		for (QualityCell cell : cells)
			if (cell.indicator == indicator && cell.selected)
				return cell.score;
		return null;
	}

	private void drawHeader(Composite comp) {
		tk.createLabel(comp, "");
		for (int i = 1; i < system.getScoreCount() + 1; i++) {
			Label label = tk.createLabel(comp, system.getScoreLabel(i));
			UI.gridData(label, false, false).horizontalAlignment = SWT.CENTER;
		}
	}

	private void drawContent(Composite comp) {
		Collections.sort(system.indicators);
		for (DQIndicator indicator : system.indicators) {
			Label rowLabel = tk.createLabel(comp, indicator.name, SWT.WRAP);
			UI.gridData(rowLabel, false, true);
			createRowData(comp, indicator);
		}
	}

	private void createRowData(Composite comp, DQIndicator indicator) {
		Collections.sort(indicator.scores);
		for (DQScore score : indicator.scores) {
			QualityCell dataCell = new QualityCell(this, indicator, score);
			dataCell.create(comp, tk);
			cells.add(dataCell);
		}
	}

	void select(DQIndicator indicator, DQScore score, boolean updateValue) {
		for (QualityCell cell : cells) {
			if (cell.indicator == indicator) {
				boolean selected = cell.score == score;
				if (selected && cell.selected)
					selected = false;
				cell.selected = selected;
				cell.setColor();
			}
		}
		if (updateValue)
			aspect.quality = getSelection();
	}

}
