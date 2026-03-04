package org.openlca.app.editors.sd.editor.graph.actions;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.editors.sd.editor.graph.SdGraphEditor;
import org.openlca.app.editors.sd.editor.graph.edit.AddVarCmd;
import org.openlca.app.editors.sd.editor.graph.edit.UpdateVarCmd;
import org.openlca.app.editors.sd.editor.graph.model.SdVarNode;
import org.openlca.app.util.Controls;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.commons.Strings;
import org.openlca.sd.model.Id;
import org.openlca.sd.model.Var;
import org.openlca.sd.model.cells.BoolCell;
import org.openlca.sd.model.cells.Cell;
import org.openlca.sd.model.cells.EqnCell;
import org.openlca.sd.model.cells.NonNegativeCell;
import org.openlca.sd.model.cells.NumCell;

class VarEditDialog extends FormDialog {

	private final SdGraphEditor editor;
	private final Var variable;
	private final Var origin;
	private final Point location;

	private Text nameText;
	private Text unitText;
	private Text equationText;
	private Button nonNegativeCheck;

	public static void edit(SdGraphEditor editor, Var origin) {
		if (editor == null || origin == null) return;
		new VarEditDialog(editor, origin).open();
	}

	public static void create(
		SdGraphEditor editor, Var variable, Point location
	) {
		if (editor == null || variable == null) return;
		var point = location != null ? location : new Point(250, 250);
		new VarEditDialog(editor, variable, point).open();
	}

	private VarEditDialog(SdGraphEditor editor, Var origin) {
		super(UI.shell());
		this.editor = editor;
		this.variable = origin.freshCopy();
		this.origin = origin;
		this.location = null;
	}

	private VarEditDialog(SdGraphEditor editor, Var variable, Point location) {
		super(UI.shell());
		this.editor = editor;
		this.variable = variable;
		this.origin = null;
		this.location = location;
	}

	@Override
	protected Point getInitialSize() {
		return new Point(500, 450);
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var tk = mForm.getToolkit();
		var body = UI.dialogBody(mForm.getForm(), tk);
		var comp = UI.composite(body, tk);
		UI.gridLayout(comp, 2);
		UI.gridData(comp, true, false);

		nameText = UI.labeledText(comp, tk, M.Name);
		if (variable.name() != null) {
			nameText.setText(variable.name().label());
		}

		unitText = UI.labeledText(comp, tk, M.Unit);
		Controls.set(unitText, variable.unit());

		equationText = UI.labeledMultiText(comp, tk, "Equation", 200);
		if (origin != null) {
			equationText.setText(initialEqn(origin.def()));
		}

		nonNegativeCheck = UI.labeledCheckbox(comp, tk, "Non-negative");
		if (origin != null) {
			nonNegativeCheck.setSelection(isNonNegative(origin.def()));
		}

		nameText.addModifyListener(e -> checkOk());
		equationText.addModifyListener(e -> checkOk());
	}

	private String initialEqn(Cell def) {
		return switch (def) {
			case BoolCell(boolean b) -> Boolean.toString(b);
			case NumCell(double num) -> Double.toString(num);
			case EqnCell(String eqn) -> eqn != null ? eqn : "";
			case NonNegativeCell(Cell value) -> initialEqn(value);
			case null, default -> "";
		};
	}

	private boolean isNonNegative(Cell def) {
		return def instanceof NonNegativeCell;
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
		if (isDuplicateName(name)) return;

		if (origin != null) {
			// TODO: when the name changed, we may need to update
			// it in other equations -> ask the user
		}

		// TODO: check equation
		var cell = Cell.of(equationText.getText());
		if (nonNegativeCheck.getSelection()) {
			cell = new NonNegativeCell(cell);
		}

		variable.setName(name);
		variable.setUnit(unitText.getText());
		variable.setDef(cell);

		if (origin == null) {
			var node = new SdVarNode(variable, editor.graph().model());
			node.moveTo(new Rectangle(location.x - 50, location.y - 25, 80, 50));
			var cmd = new AddVarCmd(editor.graph(), node);
			editor.exec(cmd);
		} else {
			var cmd = new UpdateVarCmd(editor.graph(), origin.name(), variable);
			editor.exec(cmd);
		}
		super.okPressed();
	}

	private boolean isDuplicateName(Id name) {
		if (origin != null && name.equals(origin.name())) {
			return false;
		}
		var n = editor.graph().getNode(name);
		if (n != null) {
			MsgBox.error(name.label() + " - already defined",
				"A variable with this name is already defined in this model");
			return true;
		}
		return false;
	}
}
