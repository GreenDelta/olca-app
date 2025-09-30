package org.openlca.app.navigation.actions.sd;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.M;
import org.openlca.app.editors.sd.editor.SdModelEditor;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.SdModelElement;
import org.openlca.app.rcp.images.Icon;

public class OpenSdModelAction extends Action implements INavigationAction {

	private SdModelElement elem;

	public OpenSdModelAction() {
		setText(M.Open);
		setImageDescriptor(Icon.FOLDER_OPEN.descriptor());
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		if (!(selection.getFirst() instanceof SdModelElement e))
			return false;
		elem = e;
		return true;
	}

	@Override
	public void run() {
		SdModelEditor.open(elem.getContent());
	}
}
