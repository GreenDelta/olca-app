package org.openlca.app;

import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.openlca.app.db.Database;
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
	public boolean preWindowShellClose() {
		boolean b = super.preWindowShellClose();
		if (!b)
			return false;
		App.runInUI("Close database", new Runnable() {
			public void run() {
				try {
					log.info("close database");
					Database.close();
				} catch (Exception e) {
					log.error("Failed to close database", e);
				}
			}
		});
		return true;
	}

}
