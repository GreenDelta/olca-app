package org.openlca.core.editors.lciamethod;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.openlca.core.model.LCIAFactor;

public class UncertaintyCellEditor extends DialogCellEditor {

	private LCIAFactor factor;

	public UncertaintyCellEditor(Composite parent) {
		super(parent);
	}

	@Override
	protected void doSetValue(Object value) {
		if (value instanceof LCIAFactor)
			factor = (LCIAFactor) value;
		super.doSetValue(UncertaintyLabel.get(factor));
	}

	@Override
	protected Object openDialogBox(Control control) {
		UncertaintyDialog dialog = new UncertaintyDialog(control.getShell(),
				factor);
		dialog.open();
		updateContents(UncertaintyLabel.get(factor));
		return factor;
	}

}
