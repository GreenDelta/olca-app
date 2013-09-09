package org.openlca.app.lcia_methods;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.openlca.app.components.DialogCellEditor;
import org.openlca.core.model.ImpactFactor;

public class UncertaintyCellEditor extends DialogCellEditor {

	private ImpactMethodEditor editor;
	private ImpactFactor factor;

	public UncertaintyCellEditor(Composite parent, ImpactMethodEditor editor) {
		super(parent);
		this.editor = editor;
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
		if (dialog.open() == Window.OK) {
			updateContents(UncertaintyLabel.get(factor));
			editor.setDirty(true);
			editor.postEvent(editor.IMPACT_FACTOR_CHANGE, this);
			return factor;
		}
		return null;
	}

}
