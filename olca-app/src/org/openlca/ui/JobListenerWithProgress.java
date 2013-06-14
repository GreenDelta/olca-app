/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.ui;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.openlca.core.jobs.JobHandler;
import org.openlca.core.jobs.JobListener;

/**
 * JobListenerWithProgress can be appended to a {@link JobHandler}, listens to
 * its activities and presents them in a progress monitor
 * 
 * @author Sebastian Greve
 * 
 */
public abstract class JobListenerWithProgress implements IRunnableWithProgress,
		JobListener {

	/**
	 * The monitor to present the activities of the job handler this listener is
	 * listening to
	 */
	private IProgressMonitor monitor;

	@Override
	public final void done() {
		if (monitor != null) {
			monitor.done();
		}
	}

	@Override
	public boolean isCanceled() {
		boolean canceled = false;
		if (monitor != null) {
			canceled = monitor.isCanceled();
		}
		return canceled;
	}

	@Override
	public final void jobStarted(final String name, final int length) {
		if (monitor != null) {
			monitor.beginTask(name, length);
		}
	}

	/**
	 * Define the execution commands here
	 */
	public abstract void run();

	@Override
	public final void run(final IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
		this.monitor = monitor;
		monitor.beginTask("", IProgressMonitor.UNKNOWN);
		run();
		monitor.done();
		this.monitor = null;
	}

	@Override
	public final void subJob(final String name) {
		if (monitor != null) {
			monitor.subTask(name);
		}
	}

	@Override
	public final void worked(final int amount) {
		if (monitor != null) {
			monitor.worked(amount);
		}
	}

}
