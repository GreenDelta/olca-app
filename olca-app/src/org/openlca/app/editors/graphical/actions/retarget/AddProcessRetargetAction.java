package org.openlca.app.editors.graphical.actions.retarget;

import org.eclipse.ui.actions.RetargetAction;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.actions.GraphActionIds;
import org.openlca.app.rcp.images.Images;
import org.openlca.core.model.ModelType;

public class AddProcessRetargetAction extends RetargetAction {

	public AddProcessRetargetAction() {
		super(GraphActionIds.ADD_PROCESS, M.AddProcess);
		setToolTipText(M.AddProcess);
		setImageDescriptor(Images.descriptor(ModelType.PROCESS));
	}

}
