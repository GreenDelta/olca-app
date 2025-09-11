package org.openlca.app.navigation.actions.sd;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.SdModelElement;
import org.openlca.app.navigation.elements.SdRootElement;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.FileType;
import org.openlca.app.wizards.SdModelWizard;

public class CreateSdModelAction extends Action implements INavigationAction {

	public CreateSdModelAction() {
		setText("New system dynamics model");
		setImageDescriptor(Images.descriptor(FileType.MARKUP));
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		var first = selection.getFirst();
		return first instanceof SdRootElement
				|| first instanceof SdModelElement;
	}

	@Override
	public void run() {
		SdModelWizard.open();
	}
}
