package org.openlca.app.editors.sd.editor.graph.actions.sysdialog;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.util.UI;
import org.openlca.sd.model.Id;
import org.openlca.sd.model.Var;

import java.util.List;
import java.util.Optional;

class VarSelectDialog extends FormDialog {

	private final List<Var> vars;
	private Id selected;

	static Optional<Id> selectFrom(List<Var> vars) {
		if (vars == null || vars.isEmpty()) {
			return Optional.empty();
		}
		var dialog = new VarSelectDialog(vars);
		return dialog.open() == OK && dialog.selected != null
			? Optional.of(dialog.selected)
			: Optional.empty();
	}

	private VarSelectDialog(List<Var> vars) {
		super(UI.shell());
		this.vars = vars;
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
		var panel = new VarPanel(vars, body, tk);
		panel.onSelection(id -> {
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
