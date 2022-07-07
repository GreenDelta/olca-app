package org.openlca.app.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.openlca.app.M;
import org.openlca.app.navigation.CopyPaste;
import org.openlca.app.navigation.elements.CategoryElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.ModelElement;
import org.openlca.app.rcp.images.Icon;

class CutAction extends Action implements INavigationAction {

	private List<INavigationElement<?>> selection;

	@Override
	public boolean accept(List<INavigationElement<?>> elems) {
		if (!CopyPaste.isSupported(elems))
			return false;
		for (var elem : elems) {
			if (elem instanceof ModelElement e && e.isFromLibrary()) {
				return false;
			}
			if (elem instanceof CategoryElement e && e.hasLibraryContent())
				return false;
		}
		selection = elems;
		return true;
	}

	@Override
	public void run() {
		CopyPaste.cut(selection);
	}

	@Override
	public String getText() {
		return M.Cut;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.CUT.descriptor();
	}

}
