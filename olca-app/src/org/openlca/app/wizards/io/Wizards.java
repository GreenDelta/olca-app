package org.openlca.app.wizards.io;

import java.util.function.Consumer;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.PlatformUI;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.UI;

final class Wizards {

	private Wizards() {
	}

	/**
	 * Creates the import wizard of the given ID. The given
	 * callback can be used to further configure the wizard.
	 * We do not pass in the wizard instance here because
	 * we want to let it create by the platform so that
	 * things like the platform selection etc are passed
	 * into the wizard.
	 */
	@SuppressWarnings("unchecked")
	static <T extends IImportWizard> void forImport(
			String wizardID, Consumer<T> fn) {
		if (wizardID == null)
			return;
		var descriptor = PlatformUI.getWorkbench()
				.getImportWizardRegistry()
				.findWizard(wizardID);
		if (descriptor == null) {
			ErrorReporter.on("Could not find import wizard "
					+ wizardID);
			return;
		}
		try {
			var wizard = (T) descriptor.createWizard();
			if (fn != null) {
				fn.accept(wizard);
			}
			var dialog = new WizardDialog(UI.shell(), wizard);
			dialog.setTitle(wizard.getWindowTitle());
			dialog.open();
		} catch (Exception e) {
			ErrorReporter.on("Failed to open import wizard "
					+ wizardID, e);
		}
	}
}
