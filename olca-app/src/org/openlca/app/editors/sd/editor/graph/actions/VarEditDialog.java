package org.openlca.app.editors.sd.editor.graph.actions;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.editors.sd.editor.graph.SdGraphEditor;
import org.openlca.app.editors.sd.editor.graph.edit.AddVarCmd;
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
	protected void createFormContent(IManagedForm mForm) {
		var tk = mForm.getToolkit();
		var body = UI.dialogBody(mForm.getForm(), tk);
		UI.gridLayout(body, 2);

		nameText = UI.labeledText(body, tk, M.Name);
		if (variable.name() != null) {
			nameText.setText(variable.name().label());
		}

		unitText = UI.labeledText(body, tk, M.Unit);
		Controls.set(unitText, variable.unit());

		equationText = UI.labeledMultiText(body, tk, "Equation", 200);
		if (origin != null) {
			equationText.setText(initialEqn(origin.def()));
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
			// it in other equations
		}

		// TODO: check equation
		var cell = Cell.of(equationText.getText());
		var v = origin != null ? origin : variable;
		v.setName(name);
		v.setUnit(unitText.getText());
		v.setDef(cell);

		if (origin == null && location != null) {
			var node = new SdVarNode(variable, editor.graph().model());
			node.moveTo(new Rectangle(location.x - 50, location.y - 25, 100, 50));
			var cmd = new AddVarCmd(editor.graph(), node);
			editor.exec(cmd);
		} else if (origin != null) {
			// TODO: fire some update command
		}
		super.okPressed();
	}

	private boolean isDuplicateName(Id name) {
		if (origin != null && name.equals(origin.name())) {
			return false;
		}
		for (var n : editor.graph().nodes()) {
			if (name.equals(n.variable().name())) {
				MsgBox.error(name.label() + " - already defined",
					"A variable with this name is already defined in this model");
				return true;
			}
		}
		return false;
	}
}
