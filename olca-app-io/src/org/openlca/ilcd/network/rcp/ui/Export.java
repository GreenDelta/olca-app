package org.openlca.ilcd.network.rcp.ui;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.modelprovider.IModelComponent;
import org.openlca.ilcd.io.NetworkClient;
import org.openlca.io.ilcd.output.ProcessExport;
import org.openlca.io.ilcd.output.SystemExport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Export implements IRunnableWithProgress {

	private List<ExportTupel> exportTupels;
	private Logger log = LoggerFactory.getLogger(this.getClass());
	private IProgressMonitor monitor;

	public Export(List<ExportTupel> exportTupels) {
		this.exportTupels = exportTupels;
	}

	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		beginTask(monitor);
		NetworkClient client = tryCreateClient();
		Iterator<ExportTupel> it = exportTupels.iterator();
		while (!monitor.isCanceled() && it.hasNext()) {
			ExportTupel tupel = it.next();
			monitor.subTask(tupel.getModel().getName());
			createRunExport(client, tupel);
		}
		monitor.done();
	}

	private NetworkClient tryCreateClient() throws InvocationTargetException {
		try {
			NetworkClient client = Preference.createClient();
			client.connect();
			return client;
		} catch (Exception e) {
			throw new InvocationTargetException(e, "Could not connect.");
		}
	}

	private void beginTask(IProgressMonitor monitor) {
		this.monitor = monitor;
		String taskName = "ILCD Network Export";
		monitor.beginTask(taskName, exportTupels.size() + 1);
		log.info(taskName);
	}

	private void createRunExport(NetworkClient client, ExportTupel tupel) {
		IModelComponent model = tupel.getModel();
		if (model instanceof Process)
			tryExportProcess(client, tupel);
		else if (model instanceof ProductSystem)
			tryExportSystem(client, tupel);
	}

	private void tryExportProcess(NetworkClient client, ExportTupel tupel) {
		ProcessExport export = new ProcessExport(tupel.getDatabase(), client);
		try {
			export.run((Process) tupel.getModel());
			monitor.worked(1);
		} catch (Exception e) {
			log.error("Process export failed", e);
		}
	}

	private void tryExportSystem(NetworkClient client, ExportTupel tupel) {
		SystemExport export = new SystemExport(tupel.getDatabase(), client);
		try {
			export.run((ProductSystem) tupel.getModel());
			monitor.worked(1);
		} catch (Exception e) {
			log.error("System export failed", e);
		}
	}
}
