package org.openlca.app.tools.mapping;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.components.ModelCheckBoxTree;
import org.openlca.app.util.UI;
import org.openlca.core.model.ModelType;

class ReplacerDialog2 extends FormDialog {

	private ReplacerDialog2() {
		super(UI.shell());
		setBlockOnOpen(true);
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Replace flows in database");
		UI.center(UI.shell(), shell);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		FormToolkit tk = mform.getToolkit();
		Composite root = UI.formBody(mform.getForm(), tk);
		UI.gridLayout(root, 1, 10, 10);

		UI.formLabel(root, tk, "This will replace the flows in the database " +
				"(the source system) with the flows in the target system.");

		ModelCheckBoxTree tree = new ModelCheckBoxTree(
				ModelType.PROCESS,
				ModelType.IMPACT_METHOD);
		Button delete = tk.createButton(root,
				"Delete replaced and unused flows", SWT.CHECK);

	}

	public static void main(String[] args) {
		new ReplacerDialog2().open();
	}

}
