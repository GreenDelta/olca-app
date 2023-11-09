package org.openlca.app.editors.processes.social;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
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
		UI.gridLayout(comp, system.getScoreCount() + 1, 2, 0);
		drawHeader(comp);
		drawContent(comp);
		// With the app in dark mode, it is necessary to re-set the background color
		// when painting. Otherwise, the background stay dark.
		comp.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				initSelection();
				comp.removePaintListener(this);
			}
		});
	}

	private void initSelection() {
		if (aspect.quality == null)
			return;
		int[] values = system.toValues(aspect.quality);
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
			select(indicator, score, false);
		}
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
