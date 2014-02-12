package org.openlca.app.rcp.plugins;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class PluginManagerHandler extends AbstractHandler {

	public PluginManagerHandler() {
	}

	/**
	 * the command has been executed, so extract extract the needed information
	 * from the application context.
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil
				.getActiveWorkbenchWindowChecked(event);
		PluginManagerDialog pluginManagerDialog = new PluginManagerDialog(
				window.getShell());
		pluginManagerDialog.create();
		pluginManagerDialog.getShell().setSize(500, 600);
		pluginManagerDialog.open();
		return null;
	}
}
