package org.openlca.app.editors.sd.editor.graph.actions;

import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.eclipse.swt.graphics.Point;
import org.openlca.app.editors.sd.editor.graph.SdGraphEditor;
import org.openlca.app.editors.sd.editor.graph.actions.vardialog.VarEditDialog;
import org.openlca.app.editors.sd.editor.graph.model.VarType;
import org.openlca.sd.model.Auxil;
import org.openlca.sd.model.Rate;
import org.openlca.sd.model.Stock;

import java.util.Arrays;
import java.util.List;

public class VarAddAction extends WorkbenchPartAction {

	private final VarType type;
	private final SdGraphEditor editor;
	private Point location = new Point(250, 250);

	public VarAddAction(SdGraphEditor editor, VarType type) {
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

	public static List<VarAddAction> allFor(SdGraphEditor editor) {
		return Arrays.stream(VarType.values())
			.map(type -> new VarAddAction(editor, type))
			.toList();
	}

	public static List<String> ids() {
		return Arrays.stream(VarType.values())
			.map(VarAddAction::idOf)
			.toList();
	}

	private static String idOf(VarType type) {
		return "ADD-" + type.name() + "-ACTION";
	}

	@Override
	public void run() {
		var v = switch (type) {
			case AUX ->  new Auxil();
			case RATE -> new Rate();
			case STOCK -> new Stock();
		};
		VarEditDialog.create(editor, v, location);
	}

	@Override
	protected boolean calculateEnabled() {
		location = editor.getCursorLocation();
		return true;
	}

}
