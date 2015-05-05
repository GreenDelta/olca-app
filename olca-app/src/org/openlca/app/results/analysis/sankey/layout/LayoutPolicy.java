package org.openlca.app.results.analysis.sankey.layout;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.XYLayoutEditPolicy;
import org.eclipse.gef.requests.CreateRequest;

public class LayoutPolicy extends XYLayoutEditPolicy {

	@Override
	protected Command createChangeConstraintCommand(final EditPart child,
			final Object constraint) {
		return null;
	}

	@Override
	protected Command getCreateCommand(final CreateRequest request) {
		return null;
	}
}
