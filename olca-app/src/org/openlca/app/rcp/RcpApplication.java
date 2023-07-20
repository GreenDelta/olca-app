package org.openlca.app.rcp;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.ui.PlatformUI;

public class RcpApplication implements IApplication {

	@Override
	public Object start(IApplicationContext context) throws Exception {
		var display = PlatformUI.createDisplay();
		try {
			int returnCode = PlatformUI.createAndRunWorkbench(
					display, new RcpWorkbenchAdvisor());
			return returnCode == PlatformUI.RETURN_RESTART
					? IApplication.EXIT_RESTART
					: IApplication.EXIT_OK;
		} finally {
			display.dispose();
		}
	}

	@Override
	public void stop() {
		var workbench = PlatformUI.getWorkbench();
		if (workbench == null)
			return;
		var display = workbench.getDisplay();
		display.syncExec(() -> {
			if (!display.isDisposed()) {
				workbench.close();
			}
		});
	}
}
