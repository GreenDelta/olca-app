package org.openlca.app.editors.sd.editor.graph.actions;

import org.eclipse.gef.ui.actions.SelectionAction;
import org.openlca.app.editors.sd.editor.graph.SdGraphEditor;
import org.openlca.app.editors.sd.editor.graph.edit.SystemPart;
import org.openlca.app.editors.sd.editor.graph.model.SystemNode;
import org.openlca.app.rcp.images.Icon;

public class EditSystemAction extends SelectionAction {

	public static final String ID = "EDIT-SYSTEM-ACTION";
	private final SdGraphEditor editor;
	private SystemNode node;

	public EditSystemAction(SdGraphEditor editor) {
		super(editor);
		this.editor = editor;
		setId(ID);
		setText("Edit system");
		setImageDescriptor(Icon.EDIT.descriptor());
	}

	@Override
	protected boolean calculateEnabled() {
		var selection = getSelectedEditParts();
		if (selection == null
			|| selection.size() != 1
			|| !(selection.getFirst() instanceof SystemPart part)) {
			return false;
		}
		node = part.getModel();
		return node != null;
	}

	@Override
	public void run() {
		if (node == null)
			return;
		SystemEditDialog.edit(editor, node);
	}
}
