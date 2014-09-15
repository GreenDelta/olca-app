package org.openlca.app.rcp;

import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.openlca.app.Config;
import org.openlca.app.editors.StartPage;
import org.openlca.app.util.Editors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RcpWindowAdvisor extends WorkbenchWindowAdvisor {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	public RcpWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
		super(configurer);
	}

	@Override
	public ActionBarAdvisor createActionBarAdvisor(
			IActionBarConfigurer configurer) {
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
		configurer.setTitle(Config.APPLICATION_NAME + " " + Config.VERSION);
	}

	@Override
	public void postWindowOpen() {
		if (Config.isBrowserEnabled())
			StartPage.open();
	}

	@Override
	public boolean preWindowShellClose() {
		log.trace("close all editors");
		Editors.closeAll();
		return super.preWindowShellClose();
	}

}
