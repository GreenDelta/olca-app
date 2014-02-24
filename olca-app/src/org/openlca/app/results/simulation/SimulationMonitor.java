package org.openlca.app.results.simulation;

import org.eclipse.core.runtime.IProgressMonitor;

/** The monitor of the simulation. */
class SimulationMonitor implements IProgressMonitor {

	private boolean canceled = false;
	private boolean running = false;

	@Override
	public void beginTask(String name, int totalWork) {
		running = true;
	}

	@Override
	public void done() {
		running = false;
	}

	@Override
	public void internalWorked(double work) {
	}

	@Override
	public boolean isCanceled() {
		return canceled;
	}

	@Override
	public void setCanceled(boolean value) {
		this.canceled = value;
		if (canceled)
			running = false;
	}

	@Override
	public void setTaskName(String name) {
	}

	@Override
	public void subTask(String name) {
	}

	@Override
	public void worked(int work) {
	}

	public boolean isRunning() {
		return running;
	}

}
