package org.openlca.app.ilcd_network;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.openlca.app.M;
import org.openlca.app.preferencepages.IoPreference;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.database.ProductSystemDao;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.ilcd.io.SodaClient;
import org.openlca.io.ilcd.output.ExportConfig;
import org.openlca.io.ilcd.output.ProcessExport;
import org.openlca.io.ilcd.output.SystemExport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Export implements IRunnableWithProgress {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	private List<BaseDescriptor> descriptors;
	private IProgressMonitor monitor;
	private IDatabase database;

	public Export(List<BaseDescriptor> exportTupels, IDatabase database) {
		this.descriptors = exportTupels;
		this.database = database;
	}

	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		beginTask(monitor);
		SodaClient client = tryCreateClient();
		ExportConfig config = new ExportConfig(database, client);
		config.lang = IoPreference.getIlcdLanguage();
		Iterator<BaseDescriptor> it = descriptors.iterator();
		while (!monitor.isCanceled() && it.hasNext()) {
			BaseDescriptor d = it.next();
			monitor.subTask(d.name);
			createRunExport(config, d);
		}
		monitor.done();
	}

	private SodaClient tryCreateClient() throws InvocationTargetException {
		try {
			SodaClient client = IoPreference.createClient();
			client.connect();
			return client;
		} catch (Exception e) {
			throw new InvocationTargetException(e, "Could not connect.");
		}
	}

	private void beginTask(IProgressMonitor monitor) {
		this.monitor = monitor;
		String taskName = M.ILCDNetworkExport;
		monitor.beginTask(taskName, descriptors.size() + 1);
		log.info(taskName);
	}

	private void createRunExport(ExportConfig config, BaseDescriptor d) {
		if (d.type == ModelType.PROCESS)
			tryExportProcess(config, d);
		else if (d.type == ModelType.PRODUCT_SYSTEM)
			tryExportSystem(config, d);
	}

	private void tryExportProcess(ExportConfig config, BaseDescriptor d) {
		try {
			Process p = new ProcessDao(database).getForId(d.id);
			ProcessExport export = new ProcessExport(config);
			export.run(p);
			monitor.worked(1);
		} catch (Exception e) {
			log.error("Process export failed", e);
		}
	}

	private void tryExportSystem(ExportConfig config, BaseDescriptor d) {
		try {
			ProductSystem system = new ProductSystemDao(database).getForId(d.id);
			SystemExport export = new SystemExport(config);
			export.run(system);
			monitor.worked(1);
		} catch (Exception e) {
			log.error("System export failed", e);
		}
	}
}
