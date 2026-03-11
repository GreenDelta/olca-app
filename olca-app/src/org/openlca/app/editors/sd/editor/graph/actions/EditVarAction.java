package org.openlca.app.editors.sd.editor.graph.actions;

import org.eclipse.gef.ui.actions.SelectionAction;
import org.openlca.app.editors.sd.editor.graph.SdGraphEditor;
import org.openlca.app.editors.sd.editor.graph.edit.VarPart;
import org.openlca.app.editors.sd.editor.graph.model.VarNode;
import org.openlca.app.rcp.images.Icon;

public class EditVarAction extends SelectionAction {

	public static String ID = "EDIT-VAR-ACTION";
	private final SdGraphEditor editor;
	private VarNode node;

	public EditVarAction(SdGraphEditor editor) {
		super(editor);
		this.editor = editor;
		setId(ID);
		setText("Edit");
		setImageDescriptor(Icon.EDIT.descriptor());
	}

	@Override
	protected boolean calculateEnabled() {
		var selection = getSelectedEditParts();
		if (selection == null
			|| selection.size() != 1
			|| !(selection.getFirst() instanceof VarPart part)) {
			return false;
		}
		node = part.getModel();
		return node != null;
	}

	@Override
	public void run() {
		if (node == null) return;
		VarEditDialog.edit(editor, node.variable());
	}
}
