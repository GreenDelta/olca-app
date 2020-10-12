package org.openlca.app.rcp;

import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.openlca.app.App;
import org.openlca.app.Config;
import org.openlca.app.editors.StartPage;
import org.openlca.app.preferences.Preferences;

public class RcpWindowAdvisor extends WorkbenchWindowAdvisor {

	public RcpWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
		super(configurer);
	}

	@Override
	public ActionBarAdvisor createActionBarAdvisor(IActionBarConfigurer conf) {
		return new RcpActionBarAdvisor(conf);
	}

	@Override
	public void preWindowOpen() {
		var config = getWindowConfigurer();
		config.setInitialSize(new Point(800, 600));
		config.setShowCoolBar(true);
		config.setShowStatusLine(true);
		config.setShowProgressIndicator(true);
		config.setShowMenuBar(true);
		config.setTitle(Config.APPLICATION_NAME + " " + App.getVersion());
	}

	@Override
	public void postWindowOpen() {

		// close old editors that may are still
		// open after an openLCA crash
		var config = getWindowConfigurer();
		if (config != null) {
			var window = config.getWindow();
			if (window != null) {
				var page = window.getActivePage();
				if (page != null) {
					page.closeAllEditors(false);
				}
			}
		}

		// open the start page
		if (Preferences.getBool("hide.welcome.page"))
			return;
		StartPage.open();
	}
}
