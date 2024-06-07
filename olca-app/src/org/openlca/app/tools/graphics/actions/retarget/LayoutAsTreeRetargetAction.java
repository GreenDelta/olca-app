package org.openlca.app.tools.graphics.actions.retarget;

import org.eclipse.ui.actions.RetargetAction;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.tools.graphics.actions.ActionIds;

public class LayoutAsTreeRetargetAction extends RetargetAction {

	public LayoutAsTreeRetargetAction() {
		super(ActionIds.LAYOUT_TREE, M.LayoutAsTree);
		setToolTipText(M.LayoutAsTree);
		setImageDescriptor(Icon.LAYOUT.descriptor());
	}

}
