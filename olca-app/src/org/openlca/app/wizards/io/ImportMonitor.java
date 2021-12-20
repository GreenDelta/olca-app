package org.openlca.app.wizards.io;

import org.eclipse.core.runtime.IProgressMonitor;
import org.openlca.app.M;
import org.openlca.core.io.Cancelable;
import org.openlca.core.io.ImportLog;
import org.openlca.util.Strings;
import org.slf4j.LoggerFactory;

record ImportMonitor(IProgressMonitor monitor) {

	static ImportMonitor of(IProgressMonitor monitor) {
		return new ImportMonitor(monitor);
	}

	void run(Cancelable process, ImportLog log) {
		log.listen(m -> {
			if (m.isInfo() && m.hasMessage()) {
				monitor.subTask(Strings.cut(m.message(), 80));
			}
		});
		monitor.beginTask(M.Import, IProgressMonitor.UNKNOWN);
		var watcher = new Thread(process);
		watcher.start();
		while (!monitor.isCanceled() && watcher.isAlive()) {
			try {
				watcher.join(5000);
				if (monitor.isCanceled()) {
					process.cancel();
					break;
				}
			} catch (InterruptedException e) {
				var sysLog = LoggerFactory.getLogger(getClass());
				sysLog.error("failed to join import thread");
			}
		}
	}
}
