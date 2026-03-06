package org.openlca.app.editors.sd.editor.graph.actions;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
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
import org.openlca.sd.model.cells.Cell;
import org.openlca.sd.model.cells.LookupCell;
import org.openlca.sd.model.cells.LookupEqnCell;
import org.openlca.sd.model.cells.NonNegativeCell;
import org.openlca.sd.model.cells.TensorCell;
import org.openlca.sd.model.cells.TensorEqnCell;

class VarEditDialog extends FormDialog {

	private static final String[] CELL_TYPES = {
		"Equation", "Lookup function", "Array"
	};
	private static final int TYPE_EQUATION = 0;
	private static final int TYPE_LOOKUP = 1;
	private static final int TYPE_ARRAY = 2;

	private final SdGraphEditor editor;
	private final Var variable;
	private final Var origin;
	private final Point location;

	private Text nameText;
	private Text unitText;
	private Button nonNegativeCheck;
	private Combo typeCombo;

	private Composite stack;
	private StackLayout stackLayout;
	private EquationPanel equationPanel;
	private LookupPanel lookupPanel;
	private TensorPanel tensorPanel;

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
		return new Point(500, 550);
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

		nonNegativeCheck = UI.labeledCheckbox(comp, tk, "Non-negative");

		typeCombo = UI.labeledCombo(comp, tk, "Type");
		typeCombo.setItems(CELL_TYPES);

		UI.filler(comp, tk);
		stack = UI.composite(comp, tk);
		UI.gridData(stack, true, true);
		stackLayout = new StackLayout();
		stack.setLayout(stackLayout);

		equationPanel = new EquationPanel(stack, tk);
		lookupPanel = new LookupPanel(stack, tk);
		tensorPanel = new TensorPanel(stack, tk);

		// set initial state from the origin variable
		var initialType = TYPE_EQUATION;
		if (origin != null) {
			var def = origin.def();
			nonNegativeCheck.setSelection(isNonNegative(def));
			initialType = cellTypeIndex(def);
			switch (initialType) {
				case TYPE_EQUATION -> equationPanel.setInput(def);
				case TYPE_LOOKUP -> lookupPanel.setInput(def);
				case TYPE_ARRAY -> tensorPanel.setInput(def);
			}
		}
		typeCombo.select(initialType);
		showPanel(initialType);

		// listeners
		Controls.onSelect(typeCombo, e -> {
			showPanel(typeCombo.getSelectionIndex());
			checkOk();
		});
		nameText.addModifyListener(e -> checkOk());
		equationPanel.equationText().addModifyListener(e -> checkOk());
	}

	private void showPanel(int type) {
		stackLayout.topControl = switch (type) {
			case TYPE_LOOKUP -> lookupPanel.composite;
			case TYPE_ARRAY -> tensorPanel.composite;
			default -> equationPanel.composite();
		};
		stack.layout(true, true);
	}

	private int cellTypeIndex(Cell def) {
		var unwrapped = def instanceof NonNegativeCell(Cell inner)
			? inner : def;
		return switch (unwrapped) {
			case LookupCell ignored -> TYPE_LOOKUP;
			case LookupEqnCell ignored -> TYPE_LOOKUP;
			case TensorCell ignored -> TYPE_ARRAY;
			case TensorEqnCell ignored -> TYPE_ARRAY;
			default -> TYPE_EQUATION;
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
		if (btn == null || nameText == null) {
			return;
		}
		btn.setEnabled(Strings.isNotBlank(nameText.getText()));
	}

	@Override
	protected void okPressed() {

		var name = Id.of(nameText.getText());
		if (isDuplicateName(name)) return;

		if (origin != null) {
			// TODO: when the name changed, we may need to update
			// it in other equations -> ask the user
		}

		var type = typeCombo.getSelectionIndex();
		var cell = switch (type) {
			case TYPE_LOOKUP -> lookupPanel.getCell();
			case TYPE_ARRAY -> tensorPanel.getCell();
			default -> equationPanel.getCell();
		};

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
