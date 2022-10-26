package org.openlca.app.editors.graphical.actions.retarget;

import org.eclipse.ui.actions.RetargetAction;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.actions.GraphActionIds;
import org.openlca.app.rcp.images.Icon;

public class EditStickyNoteRetargetAction extends RetargetAction {

	public EditStickyNoteRetargetAction() {
		super(GraphActionIds.EDIT_STICKY_NOTE, M.EditStickyNote);
		setToolTipText(M.EditStickyNote);
		setImageDescriptor(Icon.EDIT.descriptor());
	}

}
