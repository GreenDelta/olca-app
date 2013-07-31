package org.openlca.app.component;

import org.openlca.core.jobs.IProgressMonitor;

/**
 * A simple adapter for the openLCA IProgressMonitor that delegates all method
 * calls to the Eclipse IProgressMonitor.
 */
public class ProgressAdapter implements IProgressMonitor {

	private final org.eclipse.core.runtime.IProgressMonitor eclipseMonitor;

	public ProgressAdapter(
			org.eclipse.core.runtime.IProgressMonitor eclipseMonitor) {
		this.eclipseMonitor = eclipseMonitor;
	}

	@Override
	public void beginTask(String name, int totalWork) {
		eclipseMonitor.beginTask(name, totalWork);
	}

	@Override
	public void done() {
		eclipseMonitor.done();
	}

	@Override
	public boolean isCanceled() {
		return eclipseMonitor.isCanceled();
	}

	@Override
	public void setCanceled(boolean value) {
		eclipseMonitor.setCanceled(value);
	}

	@Override
	public void setTaskName(String name) {
		eclipseMonitor.setTaskName(name);
	}

	@Override
	public void subTask(String name) {
		eclipseMonitor.subTask(name);
	}

	@Override
	public void worked(int work) {
		eclipseMonitor.worked(work);
	}

}
