package org.openlca.app.tools.graphics.actions.retarget;

import org.eclipse.ui.actions.RetargetAction;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.tools.graphics.actions.ActionIds;

public class FocusRetargetAction extends RetargetAction {

	public FocusRetargetAction() {
		super(ActionIds.FOCUS, M.Focus);
		setToolTipText(M.Focus);
		setImageDescriptor(Icon.TARGET.descriptor());
	}

}
