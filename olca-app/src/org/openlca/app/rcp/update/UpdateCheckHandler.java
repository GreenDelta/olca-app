package org.openlca.app.rcp.update;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

/**
 * Handler that runs an update check right away.
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class UpdateCheckHandler extends AbstractHandler {

	public UpdateCheckHandler() {
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		UpdateCheckAndPrepareJob updateCheckAndPrepareJob = new UpdateCheckAndPrepareJob();
		updateCheckAndPrepareJob.setForceCheck(true);
		updateCheckAndPrepareJob.schedule();
		return null;
	}
}
