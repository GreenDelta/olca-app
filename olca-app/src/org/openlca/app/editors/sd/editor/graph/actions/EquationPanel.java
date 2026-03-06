package org.openlca.app.editors.sd.editor.graph.actions;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.util.UI;
import org.openlca.sd.model.cells.Cell;

final class EquationPanel implements Panel {

	private final Composite composite;
	private final StyledText text;

	EquationPanel(Composite parent, FormToolkit tk) {
		composite = UI.composite(parent, tk);
		UI.gridLayout(composite, 1, 5, 0);
		UI.gridData(composite, true, true);
		text = new StyledText(composite, SWT.BORDER | SWT.MULTI);
		tk.adapt(text);
		UI.gridData(text, true, true);
	}

	@Override
	public Composite composite() {
		return composite;
	}

	@Override
	public void setInput(Cell cell) {
		text.setText(eqnOf(cell));
	}

	@Override
	public Cell getCell() {
		return Cell.of(text.getText());
	}

	StyledText equationText() {
		return text;
	}
}
