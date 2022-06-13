package org.openlca.app.editors.graphical_legacy.policy;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.XYLayoutEditPolicy;
import org.eclipse.gef.requests.CreateRequest;

public class LayoutPolicy extends XYLayoutEditPolicy {

	@Override
	protected Command createChangeConstraintCommand(EditPart child, Object constraint) {
		return null;
	}

	@Override
	protected Command getCreateCommand(CreateRequest request) {
		return null;
	}
}
