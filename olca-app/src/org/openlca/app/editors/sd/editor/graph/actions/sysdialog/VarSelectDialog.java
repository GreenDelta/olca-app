package org.openlca.app.editors.sd.editor.graph.actions.sysdialog;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.util.UI;
import org.openlca.sd.model.Id;
import org.openlca.sd.model.SdModel;

import java.util.Optional;

class VarSelectDialog extends FormDialog {

	private final SdModel model;
	private Id selected;

	static Optional<Id> selectFrom(SdModel model) {
		if (model == null) {
			return Optional.empty();
		}
		var dialog = new VarSelectDialog(model);
		return dialog.open() == OK && dialog.selected != null
			? Optional.of(dialog.selected)
			: Optional.empty();
	}

	private VarSelectDialog(SdModel model) {
		super(UI.shell());
		this.model = model;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Select a variable");
	}

	@Override
	protected Point getInitialSize() {
		return new Point(500, 500);
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var tk = mForm.getToolkit();
		var body = UI.dialogBody(mForm.getForm(), tk);
		UI.gridLayout(body, 1);
		var panel = new VarPanel(model, body, tk);
		panel.onSelect(id -> {
			selected = id;
			checkOk();
		});
		checkOk();
	}

	private void checkOk() {
		var btn = getButton(IDialogConstants.OK_ID);
		if (btn != null) {
			btn.setEnabled(selected != null);
		}
	}
}
