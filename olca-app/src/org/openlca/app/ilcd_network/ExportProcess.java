package org.openlca.app.ilcd_network;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.openlca.app.M;
import org.openlca.app.preferences.IoPreference;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.io.ilcd.output.Export;
import org.openlca.io.ilcd.output.ProcessExport;
import org.openlca.io.ilcd.output.SystemExport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class ExportProcess implements IRunnableWithProgress {

	private final Logger log = LoggerFactory.getLogger(this.getClass());
	private final List<Descriptor> descriptors;
	private IProgressMonitor monitor;
	private final IDatabase db;

	public ExportProcess(List<Descriptor> descriptors, IDatabase db) {
		this.descriptors = descriptors;
		this.db = db;
	}

	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {

		this.monitor = monitor;
		String taskName = M.ILCDNetworkExport;
		monitor.beginTask(taskName, descriptors.size() + 1);
		try (var client = IoPreference.createClient()) {
			var export = new Export(db, client)
					.withLang(IoPreference.getIlcdLanguage());
			for (var d : descriptors) {
				if (monitor.isCanceled())
					break;
				monitor.subTask(d.name);
				if (d.type == ModelType.PROCESS) {
					tryExportProcess(export, d);
				} else if (d.type == ModelType.PRODUCT_SYSTEM) {
					tryExportSystem(export, d);
				}
			}
			monitor.done();
		}
	}

	private void tryExportProcess(Export exp, Descriptor d) {
		try {
			var p = db.get(Process.class, d.id);
			new ProcessExport(exp).write(p);
			monitor.worked(1);
		} catch (Exception e) {
			log.error("Process export failed", e);
		}
	}

	private void tryExportSystem(Export exp, Descriptor d) {
		try {
			var system = db.get(ProductSystem.class, d.id);
			new SystemExport(exp).write(system);
			monitor.worked(1);
		} catch (Exception e) {
			log.error("System export failed", e);
		}
	}
}
