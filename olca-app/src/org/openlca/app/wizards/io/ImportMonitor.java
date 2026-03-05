package org.openlca.app.wizards.io;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.IProgressMonitor;
import org.openlca.app.M;
import org.openlca.app.util.ErrorReporter;
import org.openlca.core.io.ImportLog.State;
import org.openlca.io.Import;

record ImportMonitor(IProgressMonitor monitor) {

	static ImportMonitor on(IProgressMonitor monitor) {
		return new ImportMonitor(monitor);
	}

	void run(Import imp) {

		imp.log().listen(message -> {
			if (message.state() == null)
				return;
			if (message.state() == State.INFO) {
				if (message.hasMessage()) {
					monitor.subTask(message.message());
				}
			}
		});

		monitor.beginTask(M.Import, IProgressMonitor.UNKNOWN);
		var watcher = new Thread(imp);
		watcher.start();

		var wasCanceled = new AtomicBoolean(false);
		while (watcher.isAlive()) {
			try {
				watcher.join(5000);
				if (monitor.isCanceled() && !wasCanceled.get()) {
					wasCanceled.set(true);
					imp.cancel();
					break;
				}
			} catch (InterruptedException e) {
				ErrorReporter.on("failed to join import thread", e);
			}
		}
		monitor.done();
		ImportLogDialog.show(imp);
	}

}
