package org.openlca.app.ilcd_network;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.openlca.app.Messages;
import org.openlca.app.preferencepages.IoPreference;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.ilcd.io.NetworkClient;
import org.openlca.ilcd.util.IlcdConfig;
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
		NetworkClient client = tryCreateClient();
		ExportConfig config = new ExportConfig(database, client);
		config.ilcdConfig = new IlcdConfig(IoPreference.getIlcdLanguage());
		Iterator<BaseDescriptor> it = descriptors.iterator();
		while (!monitor.isCanceled() && it.hasNext()) {
			BaseDescriptor descriptor = it.next();
			monitor.subTask(descriptor.getName());
			createRunExport(config, descriptor);
		}
		monitor.done();
	}

	private NetworkClient tryCreateClient() throws InvocationTargetException {
		try {
			NetworkClient client = IoPreference.createClient();
			client.connect();
			return client;
		} catch (Exception e) {
			throw new InvocationTargetException(e, "Could not connect.");
		}
	}

	private void beginTask(IProgressMonitor monitor) {
		this.monitor = monitor;
		String taskName = Messages.ILCDNetworkExport;
		monitor.beginTask(taskName, descriptors.size() + 1);
		log.info(taskName);
	}

	private void createRunExport(ExportConfig config, BaseDescriptor descriptor) {
		if (descriptor.getModelType() == ModelType.PROCESS)
			tryExportProcess(config, descriptor);
		else if (descriptor.getModelType() == ModelType.PRODUCT_SYSTEM)
			tryExportSystem(config, descriptor);
	}

	private void tryExportProcess(ExportConfig config, BaseDescriptor descriptor) {
		try {
			Process process = database.createDao(Process.class).getForId(
					descriptor.getId());
			ProcessExport export = new ProcessExport(config);
			export.run(process);
			monitor.worked(1);
		} catch (Exception e) {
			log.error("Process export failed", e);
		}
	}

	private void tryExportSystem(ExportConfig config, BaseDescriptor descriptor) {
		try {
			ProductSystem system = database.createDao(ProductSystem.class)
					.getForId(descriptor.getId());
			SystemExport export = new SystemExport(config);
			export.run(system);
			monitor.worked(1);
		} catch (Exception e) {
			log.error("System export failed", e);
		}
	}
}
