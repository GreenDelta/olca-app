package org.openlca.ilcd.network.rcp.ui;

import static org.openlca.ilcd.network.rcp.ui.Messages.NetworkImport;
import static org.openlca.ilcd.network.rcp.ui.Messages.RunImport;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.navigation.Navigator;
import org.openlca.core.database.IDatabase;
import org.openlca.ilcd.descriptors.ProcessDescriptor;
import org.openlca.ilcd.io.NetworkClient;
import org.openlca.ilcd.processes.Process;
import org.openlca.ilcd.util.ProcessBag;
import org.openlca.io.ilcd.input.ProcessImport;
import org.openlca.io.ilcd.input.SystemImport;
import org.openlca.io.ui.Activator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The wizard for the import of processes from an ILCD network.
 * 
 * @author Michael Srocka
 * 
 */
public class ImportWizard extends Wizard implements IImportWizard {

	private ProcessSearchPage processSearchPage;
	private DatabaseSelectionPage databasePage;
	private Logger log = LoggerFactory.getLogger(this.getClass());

	public ImportWizard() {
		super();
	}

	@Override
	public boolean performFinish() {
		boolean noError = true;
		try {
			final List<ProcessDescriptor> processes = processSearchPage
					.getSelectedProcesses();
			final IDatabase database = databasePage.getSelectedDatabase();
			final NetworkClient client = Preference.createClient();
			client.connect();

			getContainer().run(true, true, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					monitor.beginTask(RunImport, IProgressMonitor.UNKNOWN);
					try {
						importProcesses(processes, client, database);
					} catch (Exception e) {
						throw new InvocationTargetException(e);
					}
					monitor.done();
				}
			});
		} catch (Exception e) {
			log.error("Process import failed.", e);
			noError = false;
		}

		Navigator.refresh();

		return noError;
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
		setWindowTitle(NetworkImport);
		setDefaultPageImageDescriptor(Activator.imageDescriptorFromPlugin(
				Activator.PLUGIN_ID, "/icons/network_wiz.png")); //$NON-NLS-1$
		setNeedsProgressMonitor(true);
		processSearchPage = new ProcessSearchPage();
		databasePage = new DatabaseSelectionPage();
	}

	@Override
	public void addPages() {
		super.addPages();
		addPage(processSearchPage);
		addPage(databasePage);
	}

	@Override
	public boolean canFinish() {
		return databasePage.getSelectedDatabase() != null
				&& processSearchPage.getSelectedProcesses().size() > 0;
	}

}
