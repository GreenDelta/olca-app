package org.openlca.app.ilcd_network;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.preferencepages.IoPreference;
import org.openlca.app.rcp.RcpActivator;
import org.openlca.core.database.IDatabase;
import org.openlca.ilcd.descriptors.ProcessDescriptor;
import org.openlca.ilcd.io.DataStoreException;
import org.openlca.ilcd.io.SodaClient;
import org.openlca.ilcd.processes.Process;
import org.openlca.io.ilcd.input.ImportConfig;
import org.openlca.io.ilcd.input.ProcessImport;
import org.openlca.io.ilcd.input.ProviderLinker;
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
		List<ProcessDescriptor> processes = processSearchPage
				.getSelectedProcesses();
		SodaClient client = IoPreference.createClient();
		ImportConfig config = new ImportConfig(client, database);
		config.langs = new String[] { IoPreference.getIlcdLanguage(), "en" };
		client.connect();
		getContainer().run(true, true, monitor -> {
			monitor.beginTask(M.ILCD_RunImport, IProgressMonitor.UNKNOWN);
			try {
				importProcesses(processes, config);
			} catch (Exception e) {
				throw new InvocationTargetException(e);
			}
			monitor.done();
		});
	}

	private void importProcesses(List<ProcessDescriptor> descriptors,
			ImportConfig config) throws Exception {
		ProviderLinker linker = new ProviderLinker();
		for (ProcessDescriptor d : descriptors) {
			Process p = config.store.get(Process.class, d.uuid);
			if (p != null) {
				ProcessImport imp = new ProcessImport(config, linker);
				imp.run(p);
			}
		}
		linker.createLinks(config.db);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle(M.ILCD_NetworkImport);
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
