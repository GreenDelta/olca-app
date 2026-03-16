package org.openlca.app.editors.sd.editor.graph.actions.sysdialog;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.util.UI;
import org.openlca.commons.Strings;
import org.openlca.sd.model.Id;
import org.openlca.sd.model.Var;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class VarSelectDialog extends FormDialog {

	private final List<Id> all;
	private Id selected;

	static Optional<Id> selectFrom(List<Var> vars) {
		if (vars == null || vars.isEmpty()) {
			return Optional.empty();
		}
		var all = new ArrayList<Id>(vars.size());
		for (var v : vars) {
			all.add(v.name());
		}
		all.sort((i, j) -> Strings.compareIgnoreCase(i.label(), j.label()));
		var dialog = new VarSelectDialog(all);
		return dialog.open() == OK && dialog.selected != null
			? Optional.of(dialog.selected)
			: Optional.empty();
	}

	private VarSelectDialog(List<Id> all) {
		super(UI.shell());
		this.all = all;
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
		UI.gridLayout(body, 2, 10, 0);
		
	}

}
