package org.openlca.app.components.graphics.actions.retarget;

import org.eclipse.ui.actions.RetargetAction;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.components.graphics.actions.ActionIds;

public class EditConfigRetargetAction extends RetargetAction {

	public EditConfigRetargetAction() {
		super(ActionIds.EDIT_CONFIG, M.Settings);
		setToolTipText(M.Settings);
		setImageDescriptor(Icon.PREFERENCES.descriptor());
	}

}
