package org.openlca.app.ilcd_network;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.RcpActivator;
import org.openlca.core.database.IDatabase;
import org.openlca.ilcd.descriptors.ProcessDescriptor;
import org.openlca.ilcd.io.DataStoreException;
import org.openlca.ilcd.io.NetworkClient;
import org.openlca.ilcd.processes.Process;
import org.openlca.ilcd.util.ProcessBag;
import org.openlca.io.ilcd.input.ProcessImport;
import org.openlca.io.ilcd.input.SystemImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The wizard for the import of processes from an ILCD network node.
 */
public class ImportWizard extends Wizard implements IImportWizard {

	private IDatabase database = Database.get();
	private ProcessSearchPage processSearchPage;
	private Logger log = LoggerFactory.getLogger(this.getClass());

	public ImportWizard() {
		super();
	}

	@Override
	public boolean performFinish() {
		if (database == null)
			return false;
		boolean noError = true;
		try {
			tryImport();
		} catch (Exception e) {
			log.error("Process import failed.", e);
			noError = false;
		}
		Navigator.refresh();
		return noError;
	}

	private void tryImport() throws DataStoreException,
			InvocationTargetException, InterruptedException {
		final List<ProcessDescriptor> processes = processSearchPage
				.getSelectedProcesses();
		final NetworkClient client = Preference.createClient();
		client.connect();
		getContainer().run(true, true, new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor)
					throws InvocationTargetException, InterruptedException {
				monitor.beginTask(Messages.ILCD_RunImport,
						IProgressMonitor.UNKNOWN);
				try {
					importProcesses(processes, client, database);
				} catch (Exception e) {
					throw new InvocationTargetException(e);
				}
				monitor.done();
			}
		});
	}

	private void importProcesses(List<ProcessDescriptor> descriptors,
			NetworkClient client, IDatabase database) throws Exception {
		for (ProcessDescriptor descriptor : descriptors) {
			Process process = client.get(Process.class, descriptor.getUuid());
			if (process != null) {
				ProcessBag bag = new ProcessBag(process);
				if (bag.hasProductModel()) {
					SystemImport systemImport = new SystemImport(client,
							database);
					systemImport.run(process);
				} else {
					ProcessImport processImport = new ProcessImport(client,
							database);
					processImport.run(process);
				}
			}
		}
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle(Messages.ILCD_NetworkImport);
		setDefaultPageImageDescriptor(RcpActivator.imageDescriptorFromPlugin(
				RcpActivator.PLUGIN_ID, "/icons/network_wiz.png"));
		setNeedsProgressMonitor(true);
		processSearchPage = new ProcessSearchPage();
	}

	@Override
	public void addPages() {
		super.addPages();
		addPage(processSearchPage);
	}

	@Override
	public boolean canFinish() {
		return database != null
				&& processSearchPage.getSelectedProcesses().size() > 0;
	}
}
