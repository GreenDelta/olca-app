package org.openlca.app.editors.sd.editor.graph.actions;

import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.M;
import org.openlca.app.util.UI;
import org.openlca.app.editors.sd.editor.graph.SdGraphEditor;

public class AddRateAction extends WorkbenchPartAction {

	public static String ID = "sd-add-rate-action";

	public AddRateAction(SdGraphEditor editor) {
		super(editor);
		setId(ID);
		setText("Add rate");
	}

	@Override
	public void run() {
		new Dialog().open();
	}

	@Override
	protected boolean calculateEnabled() {
		return true;
	}

	private class Dialog extends FormDialog {

		private Text nameText;
		private Text equationText;

		Dialog() {
			super(UI.shell());
		}

		@Override
		protected void createFormContent(IManagedForm mForm) {
			var tk = mForm.getToolkit();
			var body = UI.dialogBody(mForm.getForm(), tk);
			UI.gridLayout(body, 2);

			UI.label(body, tk, M.Name);
			nameText = UI.text(body, SWT.BORDER);
			UI.gridData(nameText, true, false);

			UI.label(body, tk, "Equation");
			equationText = UI.text(body, SWT.BORDER);
			UI.gridData(equationText, true, false);
		}

		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			createButton(parent, OK, M.Ok, true);
			createButton(parent, CANCEL, M.Cancel, false);
		}

		@Override
		protected void okPressed() {
			// TODO: Implement command execution here later
			super.okPressed();
		}
	}
}
