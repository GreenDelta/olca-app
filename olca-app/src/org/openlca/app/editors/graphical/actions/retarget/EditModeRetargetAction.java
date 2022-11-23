package org.openlca.app.editors.graphical.actions.retarget;

import org.eclipse.jface.action.IAction;
import org.eclipse.ui.actions.RetargetAction;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.actions.GraphActionIds;
import org.openlca.app.rcp.images.Icon;

public class EditModeRetargetAction extends RetargetAction {

	public EditModeRetargetAction(boolean checked) {
		super(GraphActionIds.EDIT_MODE, M.EditMode, IAction.AS_CHECK_BOX);
		setToolTipText(M.ToggleEditMode);
		setImageDescriptor(Icon.EDIT.descriptor());
		setChecked(checked);
	}

}
