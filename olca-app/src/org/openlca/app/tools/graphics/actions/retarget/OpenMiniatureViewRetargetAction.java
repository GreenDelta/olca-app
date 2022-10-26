package org.openlca.app.tools.graphics.actions.retarget;

import org.eclipse.ui.actions.RetargetAction;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.tools.graphics.actions.ActionIds;

public class OpenMiniatureViewRetargetAction extends RetargetAction {

	public OpenMiniatureViewRetargetAction() {
		super(ActionIds.OPEN_MINIATURE_VIEW, M.OpenMiniatureView);
		setToolTipText(M.OpenMiniatureView);
		setImageDescriptor(Icon.MINIATURE_VIEW.descriptor());
	}

}
