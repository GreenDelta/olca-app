package org.openlca.app.components.graphics.actions.retarget;

import org.eclipse.ui.actions.RetargetAction;
import org.openlca.app.M;
import org.openlca.app.components.graphics.actions.ActionIds;
import org.openlca.app.rcp.images.Icon;

public class SaveImageRetargetAction extends RetargetAction {

	public SaveImageRetargetAction() {
		super(ActionIds.SAVE_IMAGE, M.SaveAsImage);
		setToolTipText(M.SaveAsImage);
		setImageDescriptor(Icon.SAVE_AS_IMAGE.descriptor());
	}

}
