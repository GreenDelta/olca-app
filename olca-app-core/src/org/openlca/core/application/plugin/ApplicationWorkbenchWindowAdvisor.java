/*******************************************************************************
 * Copyright (c) 2007 - 2013 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.application.plugin;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.openlca.core.application.App;
import org.openlca.core.application.navigation.DataProviderNavigationElement;
import org.openlca.core.application.navigation.INavigationElement;
import org.openlca.core.application.navigation.NavigationRoot;
import org.openlca.core.application.views.navigator.Navigator;
import org.openlca.core.database.DataProviderException;
import org.openlca.core.database.IDatabaseServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	public ApplicationWorkbenchWindowAdvisor(
			final IWorkbenchWindowConfigurer configurer) {
		super(configurer);
	}

	@Override
	public ActionBarAdvisor createActionBarAdvisor(
			final IActionBarConfigurer configurer) {
		return new ApplicationActionBarAdvisor(configurer);
	}

	@Override
	public void createWindowContents(final Shell shell) {
		super.createWindowContents(shell);
		shell.setMaximized(true);
	}

	@Override
	public void preWindowOpen() {
		final IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
		configurer.setInitialSize(new Point(800, 600));
		configurer.setShowCoolBar(true);
		configurer.setShowStatusLine(true);
		configurer.setShowProgressIndicator(true);
		configurer.setShowMenuBar(true);
		configurer.setTitle("openLCA framework " + App.getVersion());
	}

	@Override
	public boolean preWindowShellClose() {
		Navigator navigator = Navigator.getInstance();
		if (navigator != null) {
			final NavigationRoot root = navigator.getRoot();
			if (root != null) {

				for (final INavigationElement element : root.getChildren(false)) {

					if (element instanceof DataProviderNavigationElement) {
						final IDatabaseServer dataProvider = (IDatabaseServer) element
								.getData();
						if (dataProvider.isRunning()) {
							try {
								dataProvider.shutdown();
							} catch (DataProviderException e) {
								log.error("Shutdown dataprovider failed", e);
							}
						}
					}

				}

			}
		}
		return super.preWindowShellClose();
	}

}
