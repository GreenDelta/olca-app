package org.openlca.app.editors.graphical.actions.retarget;

import org.eclipse.ui.actions.RetargetAction;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.actions.GraphActionIds;
import org.openlca.app.rcp.images.Icon;

public class EditExchangeRetargetAction extends RetargetAction {

	public EditExchangeRetargetAction() {
		super(GraphActionIds.EDIT_EXCHANGE, M.EditFlow);
		setToolTipText(M.EditFlow);
		setImageDescriptor(Icon.EDIT.descriptor());
	}

}
