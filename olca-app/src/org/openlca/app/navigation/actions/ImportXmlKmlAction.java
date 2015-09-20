package org.openlca.app.navigation.actions;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.wizards.IWizardDescriptor;
import org.openlca.app.editors.locations.KmzImportWizard;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelTypeElement;
import org.openlca.app.util.UI;
import org.openlca.core.model.ModelType;

public class ImportXmlKmlAction extends Action implements INavigationAction {

	public ImportXmlKmlAction() {
		setText("#Import XML (EcoSpold2 format) geography data");
	}

	@Override
	public boolean accept(INavigationElement<?> element) {
		if (!(element instanceof ModelTypeElement))
			return false;
		ModelTypeElement mtElement = (ModelTypeElement) element;
		if (mtElement.getContent() != ModelType.LOCATION)
			return false;
		return true;
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return false;
	}

	@Override
	public void run() {
		IWizardDescriptor descriptor = PlatformUI.getWorkbench().getImportWizardRegistry()
				.findWizard(KmzImportWizard.ID);
		if (descriptor == null)
			return;
		try {
			IWizard wizard = descriptor.createWizard();
			WizardDialog dialog = new WizardDialog(UI.shell(), wizard);
			dialog.setTitle(wizard.getWindowTitle());
			dialog.open();
		} catch (CoreException e1) {
			e1.printStackTrace();
		}
	}

}
