package org.openlca.app.ilcd_network;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.RcpActivator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportWizard extends Wizard implements IExportWizard {

	private ExportWizardPage selectionPage;
	private Logger log = LoggerFactory.getLogger(this.getClass());

	public ExportWizard() {
		super();
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle(Messages.ILCDNetworkExport);
		setDefaultPageImageDescriptor(RcpActivator.imageDescriptorFromPlugin(
				RcpActivator.PLUGIN_ID, "/icons/network_wiz.png"));
		setNeedsProgressMonitor(true);
		selectionPage = new ExportWizardPage();
	}

	@Override
	public boolean performFinish() {
		boolean noException = true;
		try {
			this.getContainer().run(
					true,
					true,
					new Export(selectionPage.getSelectedModels(), Database
							.get()));
		} catch (Exception e) {
			log.error("An error occurred: " + e.getMessage(), e);
			noException = false;
		}
		return noException;
	}

	@Override
	public void addPages() {
		super.addPages();
		addPage(selectionPage);
	}

}
