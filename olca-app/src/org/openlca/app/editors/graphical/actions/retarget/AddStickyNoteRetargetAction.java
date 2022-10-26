package org.openlca.app.editors.graphical.actions.retarget;

import org.eclipse.ui.actions.RetargetAction;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.actions.GraphActionIds;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.core.model.ModelType;

public class AddStickyNoteRetargetAction extends RetargetAction {

	public AddStickyNoteRetargetAction() {
		super(GraphActionIds.ADD_STICKY_NOTE, M.AddStickyNote);
		setToolTipText(M.AddStickyNote);
		setImageDescriptor(Icon.COMMENT.descriptor());
	}

}
