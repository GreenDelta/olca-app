package org.openlca.app.components.graphics.actions.retarget;

import org.eclipse.ui.actions.RetargetAction;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.components.graphics.actions.ActionIds;

public class OpenEditorRetargetAction extends RetargetAction {

	public OpenEditorRetargetAction() {
		super(ActionIds.OPEN_EDITOR, M.OpenInEditor);
		setToolTipText(M.OpenInEditor);
		setImageDescriptor(Icon.OPEN_FOLDER.descriptor());
	}

}
