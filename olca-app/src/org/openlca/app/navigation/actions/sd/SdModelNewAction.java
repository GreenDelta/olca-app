package org.openlca.app.navigation.actions.sd;

import org.eclipse.jface.action.Action;
import org.openlca.app.editors.sd.SdModelWizard;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.SdModelElement;
import org.openlca.app.navigation.elements.SdRootElement;
import org.openlca.app.rcp.images.Icon;

import java.util.List;

public class SdModelNewAction extends Action implements INavigationAction {

	public SdModelNewAction() {
		setText("New system dynamics model");
		setImageDescriptor(Icon.ADD.descriptor());
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.size() != 1) return false;
		var first = selection.getFirst();
		return first instanceof SdRootElement || first instanceof SdModelElement;
	}

	@Override
	public void run() {
		SdModelWizard.openNew();
	}
}
