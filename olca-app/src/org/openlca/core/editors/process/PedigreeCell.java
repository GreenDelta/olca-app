package org.openlca.core.editors.process;

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
import org.openlca.app.UI;
import org.openlca.core.model.PedigreeMatrixRow;

class PedigreeCell {

	private PedigreeShell shell;
	private PedigreeMatrixRow row;
	private int score;
	private boolean selected;
	private Label label;
	private Composite composite;

	PedigreeCell(PedigreeShell shell, PedigreeMatrixRow row, int score) {
		this.shell = shell;
		this.row = row;
		this.score = score;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public boolean isSelected() {
		return selected;
	}

	public PedigreeMatrixRow getRow() {
		return row;
	}

	public int getScore() {
		return score;
	}

	void createComponents(Composite parent, PedigreeShellData data,
			FormToolkit toolkit) {
		String text = data.getLabel(row, score);
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
			label.setBackground(shell.getColors()[score - 1]);
			composite.setBackground(shell.getColors()[score - 1]);
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
			color = shell.getColors()[score - 1];
		else
			color = shell.getDisplay().getSystemColor(SWT.COLOR_WHITE);
		label.setBackground(color);
		composite.setBackground(color);
	}

	private class MouseClick extends MouseAdapter {

		@Override
		public void mouseDown(MouseEvent e) {
			shell.select(row, score);
			shell.calculateSigmaG();
		}
	}

}