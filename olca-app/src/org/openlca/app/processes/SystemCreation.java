package org.openlca.app.processes;

import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PlatformUI;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.util.UI;
import org.openlca.app.wizards.ProductSystemWizard;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opens the product system wizard with the given process as default selection.
 */
class SystemCreation {

	private Process process;

	public static void run(Process process) {
		if (process == null)
			return;
		new SystemCreation(process).doIt();
	}

	private SystemCreation(Process process) {
		this.process = process;
	}

	private void doIt() {
		try {
			String wizardId = "wizards.new.productsystem";
			IWorkbenchWizard wizard = PlatformUI.getWorkbench()
					.getNewWizardRegistry().findWizard(wizardId).createWizard();
			if (!(wizard instanceof ProductSystemWizard))
				return;
			ProductSystemWizard systemWizard = (ProductSystemWizard) wizard;
			systemWizard.setProcess(process);
			WizardDialog dialog = new WizardDialog(UI.shell(), wizard);
			if (dialog.open() == Window.OK)
				Navigator.refresh(Navigator
						.findElement(ModelType.PRODUCT_SYSTEM));
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("failed to open product system dialog for process", e);
		}
	}
}
