package org.openlca.app.editors.sd.editor.graph.actions;

import org.eclipse.gef.ui.actions.SelectionAction;
import org.openlca.app.editors.sd.editor.graph.SdGraphEditor;
import org.openlca.app.editors.sd.editor.graph.actions.vardialog.VarEditDialog;
import org.openlca.app.editors.sd.editor.graph.edit.SystemPart;
import org.openlca.app.editors.sd.editor.graph.edit.VarPart;
import org.openlca.app.editors.sd.editor.graph.model.SystemNode;
import org.openlca.app.editors.sd.editor.graph.model.VarNode;
import org.openlca.app.rcp.images.Icon;

public class EditAction extends SelectionAction {

	public static final String ID = "EDIT-ACTION";

	private final SdGraphEditor editor;
	private VarNode varNode;
	private SystemNode systemNode;

	public EditAction(SdGraphEditor editor) {
		super(editor);
		this.editor = editor;
		setId(ID);
		setText("Edit");
		setImageDescriptor(Icon.EDIT.descriptor());
	}

	@Override
	protected boolean calculateEnabled() {
		varNode = null;
		systemNode = null;

		var selection = getSelectedEditParts();
		if (selection == null || selection.size() != 1) {
			return false;
		}

		var first = selection.getFirst();
		if (first instanceof VarPart part) {
			varNode = part.getModel();
			return varNode != null;
		}
		if (first instanceof SystemPart part) {
			systemNode = part.getModel();
			return systemNode != null;
		}
		return false;
	}

	@Override
	public void run() {
		if (varNode != null) {
			VarEditDialog.edit(editor, varNode.variable());
			return;
		}
		if (systemNode != null) {
			SystemEditDialog.edit(editor, systemNode);
		}
	}
}
