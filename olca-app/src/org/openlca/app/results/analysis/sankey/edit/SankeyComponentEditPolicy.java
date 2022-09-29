package org.openlca.app.results.analysis.sankey.edit;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.UnexecutableCommand;
import org.eclipse.gef.editpolicies.ComponentEditPolicy;
import org.eclipse.gef.requests.GroupRequest;

public class SankeyComponentEditPolicy extends ComponentEditPolicy {

	/**
	 * Overridden to prevent the host from being deleted.
	 *
	 * @see org.eclipse.gef.editpolicies.ComponentEditPolicy#createDeleteCommand(GroupRequest)
	 */
	protected Command createDeleteCommand(GroupRequest request) {
		return UnexecutableCommand.INSTANCE;
	}

}
