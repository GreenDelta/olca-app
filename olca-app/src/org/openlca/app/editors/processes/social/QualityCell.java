package org.openlca.app.editors.processes.social;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.util.Colors;
import org.openlca.app.util.UI;
import org.openlca.core.model.PedigreeMatrixRow;

class QualityCell {

	final PedigreeMatrixRow row;
	final int score;
	boolean selected;

	private final QualityPanel panel;
	private Label label;
	private Composite composite;

	public QualityCell(QualityPanel panel, PedigreeMatrixRow row, int score) {
		this.panel = panel;
		this.row = row;
		this.score = score;
	}

	void create(Composite parent, FormToolkit tk, QualityLabelData data) {
		String text = data.getLabel(row, score);
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
	}

	private class MouseOver implements MouseTrackListener {
		@Override
		public void mouseEnter(MouseEvent e) {
			label.setBackground(QualityLabelData.getColors()[score - 1]);
			composite.setBackground(QualityLabelData.getColors()[score - 1]);
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
			color = QualityLabelData.getColors()[score - 1];
		else
			color = Colors.white();
		label.setBackground(color);
		composite.setBackground(color);
	}

	private class MouseClick extends MouseAdapter {

		@Override
		public void mouseDown(MouseEvent e) {
			panel.select(row, score, true);
		}
	}
}
