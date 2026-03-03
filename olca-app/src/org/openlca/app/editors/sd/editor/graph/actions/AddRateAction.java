package org.openlca.app.editors.sd.editor.graph.actions;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.editors.sd.editor.graph.AddVarCmd;
import org.openlca.app.editors.sd.editor.graph.SdGraphEditor;
import org.openlca.app.editors.sd.editor.graph.model.SdVarNode;
import org.openlca.app.util.UI;
import org.openlca.commons.Strings;
import org.openlca.sd.model.Id;
import org.openlca.sd.model.Rate;
import org.openlca.sd.model.cells.Cell;

public class AddRateAction extends WorkbenchPartAction {

	public static String ID = "sd-add-rate-action";
	private final SdGraphEditor editor;

	public AddRateAction(SdGraphEditor editor) {
		super(editor);
		this.editor = editor;
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
		private Text unitText;
		private Text equationText;

		Dialog() {
			super(UI.shell());
		}

		@Override
		protected void createFormContent(IManagedForm mForm) {
			var tk = mForm.getToolkit();
			var body = UI.dialogBody(mForm.getForm(), tk);
			UI.gridLayout(body, 2);
			nameText = UI.labeledText(body, tk, M.Name);
			unitText = UI.labeledText(body, tk, M.Unit);
			equationText = UI.labeledMultiText(body, tk, "Equation", 200);

			nameText.addModifyListener(e -> checkOk());
			equationText.addModifyListener(e -> checkOk());
			checkOk();
		}

		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			createButton(parent, OK, M.OK, true);
			createButton(parent, CANCEL, M.Cancel, false);
		}

		private void checkOk() {
			var btn = getButton(OK);
			if (btn == null || nameText == null || equationText == null) {
				return;
			}
			var isOk = Strings.isNotBlank(nameText.getText())
				&& Strings.isNotBlank(equationText.getText());
			btn.setEnabled(isOk);
		}

		@Override
		protected void okPressed() {
			var rate = new Rate();
			rate.setName(Id.of(nameText.getText().trim()));
			rate.setUnit(unitText.getText().trim());
			rate.setDef(Cell.of(equationText.getText().trim()));

			var p = editor.getCursorLocation();
			var m = new SdVarNode(rate, editor.graph().model());
			m.moveTo(new Rectangle(p.x, p.y, 100, 50));

			var cmd = new AddVarCmd(
				editor.parent().model(),
				editor.graph(),
				m);
			editor.exec(cmd);

			super.okPressed();
		}
	}
}
