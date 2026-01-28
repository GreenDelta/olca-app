package org.openlca.app.editors.graphical.actions.retarget;

import org.eclipse.ui.actions.RetargetAction;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.GraphActionIds;
import org.openlca.app.rcp.images.Images;
import org.openlca.core.model.ModelType;

public class AddExchangeRetargetAction extends RetargetAction {

	public AddExchangeRetargetAction(boolean forInput) {
		super(
				forInput
						? GraphActionIds.ADD_INPUT
						: GraphActionIds.ADD_OUTPUT,
				forInput
						? M.AddInputFlow
						: M.AddOutputFlow
		);
		setToolTipText(forInput ? M.AddInputFlow : M.AddOutputFlow);
		setImageDescriptor(Images.descriptor(ModelType.FLOW));
	}

}
