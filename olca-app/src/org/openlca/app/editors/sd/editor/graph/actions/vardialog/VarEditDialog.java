package org.openlca.app.editors.sd.editor.graph.actions.vardialog;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.editors.sd.editor.graph.SdGraphEditor;
import org.openlca.app.editors.sd.editor.graph.edit.VarAddCmd;
import org.openlca.app.editors.sd.editor.graph.edit.VarUpdateCmd;
import org.openlca.app.editors.sd.editor.graph.model.VarNode;
import org.openlca.app.util.Controls;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Question;
import org.openlca.app.util.UI;
import org.openlca.commons.Strings;
import org.openlca.sd.model.Id;
import org.openlca.sd.model.Stock;
import org.openlca.sd.model.Var;
import org.openlca.sd.model.cells.NonNegativeCell;

public class VarEditDialog extends FormDialog {

	private final SdGraphEditor editor;
	private final Var variable;
	private final Var origin;
	private final Point location;

	private Text nameText;
	private Text unitText;
	private Button nonNegativeCheck;
	private PanelStack panels;
	private boolean panelFinished;

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
		this.panelFinished = true;
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
		return new Point(600, 625);
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
		nameText.addModifyListener(e -> checkOk());

		unitText = UI.labeledText(comp, tk, M.Unit);
		Controls.set(unitText, variable.unit());
		nonNegativeCheck = UI.labeledCheckbox(comp, tk, "Non-negative");
		nonNegativeCheck.setSelection(
			variable.def() instanceof NonNegativeCell);

		boolean isStockVar = false;
		if (variable instanceof Stock stock) {
			isStockVar = true;
			new StockFlowPanel(editor.graph().model(), stock)
				.render(comp, tk, this::checkOk);
		}

		panels = new PanelStack(comp, tk, isStockVar);
		panels.setInput(variable.def());
		panels.onChange(b -> {
			panelFinished = b;
			checkOk();
		});
		mForm.getForm().reflow(true);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, OK, M.OK, true).setEnabled(false);
		createButton(parent, CANCEL, M.Cancel, false);
	}

	private void checkOk() {
		var btn = getButton(OK);
		if (btn == null || nameText == null) {
			return;
		}
		btn.setEnabled(panelFinished &&
			Strings.isNotBlank(nameText.getText()));
	}

	@Override
	protected void okPressed() {

		var name = Id.of(nameText.getText());
		if (isDuplicateName(name)) return;

		if (origin != null && !name.equals(origin.name())) {
			var b = Question.ask(
				"Rename variable?",
				"Renaming the variable will also update mathematical expressions " +
					"where it is used. Do you want to rename it?");
			if (!b) return;
		}

		var cell = nonNegativeCheck.getSelection()
			? new NonNegativeCell(panels.getCell())
			: panels.getCell();
		variable.setName(name);
		variable.setUnit(unitText.getText());
		variable.setDef(cell);

		if (origin == null) {
			var node = new VarNode(variable, editor.graph().model());
			node.moveTo(new Rectangle(location.x - 50, location.y - 25, 80, 50));
			var cmd = new VarAddCmd(editor.graph(), node);
			editor.exec(cmd);
		} else {
			var cmd = new VarUpdateCmd(editor.graph(), origin.name(), variable);
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
