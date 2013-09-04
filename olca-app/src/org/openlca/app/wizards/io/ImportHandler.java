package org.openlca.app.wizards.io;

import org.eclipse.core.runtime.IProgressMonitor;
import org.openlca.io.FileImport;
import org.openlca.io.ImportEvent;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

public class ImportHandler {

	private Logger log = LoggerFactory.getLogger(getClass());
	private IProgressMonitor monitor;
	private int cancelLookUpTime = 5000;

	public ImportHandler(IProgressMonitor monitor) {
		this.monitor = monitor;
	}

	@Subscribe
	public void handleEvent(ImportEvent event) {
		monitor.subTask(Strings.cut(event.getDataSetName(), 50));
	}

	public void run(FileImport fileImport) {
		EventBus bus = new EventBus();
		bus.register(this);
		fileImport.setEventBus(bus);
		Thread thread = new Thread(fileImport);
		thread.start();
		while (!monitor.isCanceled() && thread.isAlive())
			try {
				thread.join(cancelLookUpTime);
				if (monitor.isCanceled())
					fileImport.cancel();
			} catch (Exception e) {
				log.error("failed to join import thread");
			}
	}

}
