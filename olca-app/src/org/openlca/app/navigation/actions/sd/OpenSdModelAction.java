package org.openlca.app.navigation.actions.sd;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.editors.sd.SdModelEditor;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.SdModelElement;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.FileType;

public class OpenSdModelAction extends Action implements INavigationAction {

	public OpenSdModelAction() {
		setText("Open");
		setImageDescriptor(Images.descriptor(FileType.MARKUP));
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		var first = selection.getFirst();
		return first instanceof SdModelElement;
	}

	@Override
	public void run() {
		// This will be called by the navigation framework
		// We need to get the selection from the navigator
	}

	public void run(SdModelElement element) {
		if (element != null && element.getContent() != null) {
			SdModelEditor.open(element.getContent());
		}
	}
}
