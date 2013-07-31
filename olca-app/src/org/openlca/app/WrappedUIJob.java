package org.openlca.app;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.ui.progress.UIJob;

class WrappedUIJob extends UIJob {

	private Runnable runnable;

	public WrappedUIJob(String name, Runnable runnable) {
		super(name);
		this.runnable = runnable;
	}

	@Override
	public IStatus runInUIThread(IProgressMonitor monitor) {
		App.log.trace("execute UI job {}", this);
		monitor.beginTask(getName(), IProgressMonitor.UNKNOWN);
		BusyIndicator.showWhile(getDisplay(), new Runnable() {
			@Override
			public void run() {
				try {
					runnable.run();
				} catch (Exception e) {
					App.log.error("UI callback failed", e);
				}
			}
		});
		return Status.OK_STATUS;
	}

}