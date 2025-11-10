package org.openlca.app.components.graphics.actions.retarget;

import org.eclipse.ui.actions.RetargetAction;
import org.openlca.app.M;
import org.openlca.app.components.graphics.actions.ActionIds;
import org.openlca.app.rcp.images.Icon;

public class MinimapRetargetAction extends RetargetAction {

	public MinimapRetargetAction(boolean checked) {
		super(ActionIds.MINIMAP, M.Minimap);
		setToolTipText(M.ToggleMinimap);
		setImageDescriptor(Icon.MINIMAP.descriptor());
		setChecked(checked);
	}

}
