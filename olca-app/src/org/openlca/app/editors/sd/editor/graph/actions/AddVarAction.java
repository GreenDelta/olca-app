package org.openlca.app.editors.sd.editor.graph.actions;

import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.eclipse.swt.graphics.Point;
import org.openlca.app.editors.sd.editor.graph.SdGraphEditor;
import org.openlca.app.editors.sd.editor.graph.model.VarType;
import org.openlca.sd.model.Auxil;
import org.openlca.sd.model.Id;
import org.openlca.sd.model.Rate;
import org.openlca.sd.model.Stock;
import org.openlca.sd.model.cells.Cell;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AddVarAction extends WorkbenchPartAction {

	private final VarType type;
	private final SdGraphEditor editor;
	private Point location = new Point(250, 250);

	public AddVarAction(SdGraphEditor editor, VarType type) {
		super(editor);
		this.editor = editor;
		this.type = type;
		setId(idOf(type));
		var label = switch (type) {
			case AUX -> "Add auxiliary";
			case RATE -> "Add rate";
			case STOCK -> "Add stock";
		};
		setText(label);
	}

	public static List<AddVarAction> allFor(SdGraphEditor editor) {
		return Arrays.stream(VarType.values())
			.map(type -> new AddVarAction(editor, type))
			.toList();
	}

	public static List<String> ids() {
		return Arrays.stream(VarType.values())
			.map(AddVarAction::idOf)
			.toList();
	}

	private static String idOf(VarType type) {
		return "ADD-" + type.name() + "-ACTION";
	}

	@Override
	public void run() {
		var def = Cell.of(1);
		var v = switch (type) {
			case AUX ->  new Auxil(Id.of("aux"), def, "");
			case RATE -> new Rate(Id.of("rate"), def, "");
			case STOCK -> new Stock(
				Id.of("stock"), def, "", new ArrayList<>(), new ArrayList<>());
		};
		VarEditDialog.create(editor, v, location);
	}

	@Override
	protected boolean calculateEnabled() {
		location = editor.getCursorLocation();
		return true;
	}

}
