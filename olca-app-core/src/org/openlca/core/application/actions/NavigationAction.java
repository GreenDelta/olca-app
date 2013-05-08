/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.application.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.PlatformUI;
import org.openlca.core.application.views.navigator.Navigator;
import org.openlca.core.jobs.Jobs;
import org.openlca.ui.JobListenerWithProgress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class for actions in the navigation view. Provides methods for
 * preparing and finalizing the action
 * 
 * @author Sebastian Greve
 * 
 */
public abstract class NavigationAction extends Action {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	protected abstract String getTaskName();

	protected abstract void task();

	public void after() {
		Navigator.refresh();
	}

	public void prepare() {
		// sub classes should implement
	}

	@Override
	public final void run() {
		prepare();
		if (getTaskName() != null) {
			try {
				PlatformUI.getWorkbench().getProgressService()
						.busyCursorWhile(new RunnableWithProgress());
			} catch (final Exception e) {
				log.error("NavigationAction error", e);
			}
		} else {
			task();
		}
		after();
	}

	private class RunnableWithProgress extends JobListenerWithProgress {

		@Override
		public void run() {
			Jobs.getHandler(Jobs.MAIN_JOB_HANDLER).addJobListener(this);
			task();
			Jobs.getHandler(Jobs.MAIN_JOB_HANDLER).removeJobListener(this);
		}

	}

}
