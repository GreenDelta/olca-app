package org.openlca.ilcd.network.rcp.ui;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.io.Activator;
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
		setWindowTitle("ILCD Network Export");
		setDefaultPageImageDescriptor(Activator.imageDescriptorFromPlugin(
				Activator.PLUGIN_ID, "/icons/network_wiz.png")); //$NON-NLS-1$
		setNeedsProgressMonitor(true);
		selectionPage = new ExportWizardPage();
	}

	@Override
	public boolean performFinish() {
		boolean noException = true;
		try {
			this.getContainer().run(true, true,
					new Export(selectionPage.getSelectedModels()));
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
