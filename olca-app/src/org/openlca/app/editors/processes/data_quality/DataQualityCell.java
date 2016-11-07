package org.openlca.app.editors.processes.data_quality;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.util.DQUI;
import org.openlca.app.util.UI;
import org.openlca.core.model.DQIndicator;
import org.openlca.core.model.DQScore;

class DataQualityCell {

	private DataQualityShell shell;
	private DQIndicator indicator;
	private DQScore score;
	private boolean selected;
	private Label label;
	private Composite composite;

	DataQualityCell(DataQualityShell shell, DQIndicator indicator, DQScore score) {
		this.shell = shell;
		this.indicator = indicator;
		this.score = score;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public boolean isSelected() {
		return selected;
	}

	public DQIndicator getIndicator() {
		return indicator;
	}

	public DQScore getScore() {
		return score;
	}

	void createComponents(Composite parent, FormToolkit toolkit) {
		String text = score.description;
		composite = toolkit.createComposite(parent, SWT.BORDER);
		GridData gridData = UI.gridData(composite, true, true);
		gridData.minimumWidth = 120;
		gridData.widthHint = 120;
		FillLayout layout = new FillLayout(SWT.HORIZONTAL);
		layout.marginHeight = 2;
		layout.marginWidth = 2;
		composite.setLayout(layout);
		label = toolkit.createLabel(composite, text, SWT.WRAP);
		label.addMouseTrackListener(new MouseOver());
		label.addMouseListener(new MouseClick());
	}

	private class MouseOver implements MouseTrackListener {
		@Override
		public void mouseEnter(MouseEvent e) {
			Color color = DQUI.getColor(score.position, indicator.scores.size());
			label.setBackground(color);
			composite.setBackground(color);
		}

		@Override
		public void mouseExit(MouseEvent e) {
			setColor();
		}

		@Override
		public void mouseHover(MouseEvent e) {
		}
	}

	void setColor() {
		Color color = null;
		if (selected)
			color = DQUI.getColor(score.position, indicator.scores.size());
		else
			color = shell.getDisplay().getSystemColor(SWT.COLOR_WHITE);
		label.setBackground(color);
		composite.setBackground(color);
	}

	private class MouseClick extends MouseAdapter {

		@Override
		public void mouseDown(MouseEvent e) {
			shell.select(indicator, score);
			shell.updateSigmaG();
		}
	}

}