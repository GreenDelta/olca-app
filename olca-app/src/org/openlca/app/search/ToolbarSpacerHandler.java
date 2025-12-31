package org.openlca.app.search;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

/**
 * A dummy handler for a spacer command in the search toolbar. This is used to
 * fix alignment issues on Windows when a ControlContribution is the first
 * element in a toolbar at high zoom levels (>200%).
 */
public class ToolbarSpacerHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// Do nothing - this is just a spacer
		return null;
	}

}

