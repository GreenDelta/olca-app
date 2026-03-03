package org.openlca.app.editors.sd.editor.graph.actions;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.editors.sd.editor.graph.SdGraphEditor;
import org.openlca.app.editors.sd.editor.graph.edit.AddVarCmd;
import org.openlca.app.editors.sd.editor.graph.model.SdVarNode;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.commons.Strings;
import org.openlca.sd.model.Id;
import org.openlca.sd.model.Rate;
import org.openlca.sd.model.cells.Cell;

public class AddRateAction extends WorkbenchPartAction {

	public static String ID = "sd-add-rate-action";
	private final SdGraphEditor editor;
	private Point location = new Point(250, 250);

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
		location = editor.getCursorLocation();
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
		}

		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			createButton(parent, OK, M.OK, true).setEnabled(false);
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

			var name = Id.of(nameText.getText());
			for (var n : editor.graph().nodes()) {
				if (name.equals(n.variable().name())) {
					MsgBox.error(name.label() + " - already defined",
						"A variable with this name is already defined in this model");
					return;
				}
			}

			var rate = new Rate();
			rate.setName(name);
			rate.setUnit(unitText.getText().trim());
			rate.setDef(Cell.of(equationText.getText().trim()));

			var node = new SdVarNode(rate, editor.graph().model());
			node.moveTo(new Rectangle(location.x - 50, location.y - 25, 100, 50));
			var cmd = new AddVarCmd(editor.graph(), node);
			editor.exec(cmd);
			super.okPressed();
		}
	}
}
