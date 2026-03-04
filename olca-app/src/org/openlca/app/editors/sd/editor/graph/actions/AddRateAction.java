package org.openlca.app.editors.sd.editor.graph.actions;

import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.eclipse.swt.graphics.Point;
import org.openlca.app.editors.sd.editor.graph.SdGraphEditor;
import org.openlca.sd.model.Rate;

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
		VarEditDialog.create(editor, new Rate(), location);
	}

	@Override
	protected boolean calculateEnabled() {
		location = editor.getCursorLocation();
		return true;
	}

}
