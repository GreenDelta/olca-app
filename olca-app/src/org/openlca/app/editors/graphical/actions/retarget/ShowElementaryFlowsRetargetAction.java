package org.openlca.app.editors.graphical.actions.retarget;

import org.eclipse.jface.action.IAction;
import org.eclipse.ui.actions.RetargetAction;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.actions.GraphActionIds;
import org.openlca.app.rcp.images.Images;
import org.openlca.core.model.FlowType;

public class ShowElementaryFlowsRetargetAction extends RetargetAction {

	public ShowElementaryFlowsRetargetAction(boolean checked) {
		super(GraphActionIds.SHOW_ELEMENTARY_FLOWS, M.ShowElementaryFlows,
				IAction.AS_CHECK_BOX);
		setToolTipText(M.ToggleShowElementaryFlows);
		setImageDescriptor(Images.descriptor(FlowType.ELEMENTARY_FLOW));
		setChecked(checked);
	}

}
