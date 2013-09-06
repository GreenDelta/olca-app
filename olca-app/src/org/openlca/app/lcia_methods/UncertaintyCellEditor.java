package org.openlca.app.lcia_methods;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.openlca.app.components.DialogCellEditor;
import org.openlca.core.model.ImpactFactor;

public class UncertaintyCellEditor extends DialogCellEditor {

	private ImpactFactor factor;

	public UncertaintyCellEditor(Composite parent) {
		super(parent);
	}

	@Override
	protected void doSetValue(Object value) {
		if (value instanceof ImpactFactor)
			factor = (ImpactFactor) value;
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
