package org.openlca.app.editors.processes.social;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.util.Colors;
import org.openlca.app.util.DQUI;
import org.openlca.app.util.UI;
import org.openlca.core.model.DQIndicator;
import org.openlca.core.model.DQScore;


class QualityCell {

	final DQIndicator indicator;
	final DQScore score;
	boolean selected;

	private final QualityPanel panel;
	private Label label;
	private Composite composite;

	public QualityCell(QualityPanel panel, DQIndicator indicator, DQScore score) {
		this.panel = panel;
		this.indicator = indicator;
		this.score = score;
	}

	void create(Composite parent, FormToolkit tk) {
		String text = score.description;
		composite = tk.createComposite(parent, SWT.BORDER);
		UI.gridData(composite, true, true);
		FillLayout layout = new FillLayout(SWT.HORIZONTAL);
		layout.marginHeight = 2;
		layout.marginWidth = 2;
		composite.setLayout(layout);
		label = tk.createLabel(composite, "", SWT.WRAP);
		label.setToolTipText(text);
		composite.setToolTipText(text);
		label.addMouseTrackListener(new MouseOver());
		label.addMouseListener(new MouseClick());
		// With the app in dark mode, it is necessary to re-set the background color
		// when painting. Otherwise, the background stay dark.
		label.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				setColor();
				label.removePaintListener(this);
			}
		});
	}

	private class MouseOver implements MouseTrackListener {
		@Override
		public void mouseEnter(MouseEvent e) {
			var color = DQUI.getColor(score.position, indicator.scores.size());
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
		var color = selected
				? DQUI.getColor(score.position, indicator.scores.size())
				: Colors.background();
		label.setBackground(color);
		composite.setBackground(color);
	}

	private class MouseClick extends MouseAdapter {

		@Override
		public void mouseDown(MouseEvent e) {
			panel.select(indicator, score, true);
		}
	}
}
