package org.openlca.app.rcp;

import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.openlca.app.App;
import org.openlca.app.Config;
import org.openlca.app.editors.StartPage;

public class RcpWindowAdvisor extends WorkbenchWindowAdvisor {

	public RcpWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
		super(configurer);
	}

	@Override
	public ActionBarAdvisor createActionBarAdvisor(IActionBarConfigurer configurer) {
		return new RcpActionBarAdvisor(configurer);
	}

	@Override
	public void preWindowOpen() {
		IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
		configurer.setInitialSize(new Point(800, 600));
		configurer.setShowCoolBar(true);
		configurer.setShowStatusLine(true);
		configurer.setShowProgressIndicator(true);
		configurer.setShowMenuBar(true);
		configurer.setTitle(Config.APPLICATION_NAME + " " + App.getVersion());
	}

	@Override
	public void postWindowOpen() {
		if (Config.isBrowserEnabled())
			if (isStandardPerspective())
				StartPage.open();
	}

	private boolean isStandardPerspective() {
		IWorkbench wb = PlatformUI.getWorkbench();
		IWorkbenchWindow win = wb.getActiveWorkbenchWindow();
		IWorkbenchPage page = win.getActivePage();
		IPerspectiveDescriptor perspective = page.getPerspective();
		return perspective.getId().equals(RcpPerspective.ID);
	}

}
