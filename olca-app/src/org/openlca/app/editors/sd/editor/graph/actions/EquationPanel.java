package org.openlca.app.editors.sd.editor.graph.actions;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.util.UI;
import org.openlca.commons.Strings;
import org.openlca.sd.model.cells.Cell;

final class EquationPanel extends Panel {

	private final StyledText text;

	EquationPanel(Composite parent, FormToolkit tk) {
		super(UI.composite(parent, tk));
		var comp = composite();
		UI.gridLayout(comp, 1, 5, 0);
		UI.gridData(comp, true, true);
		text = new StyledText(comp, SWT.BORDER | SWT.MULTI);
		tk.adapt(text);
		UI.gridData(text, true, true);
		text.addModifyListener(
			e -> fireValid(Strings.isNotBlank(text.getText())));
	}

	@Override
	public void setInput(Cell cell) {
		text.setText(eqnOf(cell));
	}

	@Override
	public Cell getCell() {
		return Cell.of(text.getText());
	}
}
