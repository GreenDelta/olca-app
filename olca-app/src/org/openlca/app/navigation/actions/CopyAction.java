package org.openlca.app.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.openlca.app.M;
import org.openlca.app.navigation.CopyPaste;
import org.openlca.app.navigation.elements.CategoryElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;

class CopyAction extends Action implements INavigationAction {

	private List<INavigationElement<?>> selection;

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		if (!CopyPaste.isSupported(elements))
			return false;
		for (var elem : elements) {
			if (elem instanceof CategoryElement ce && ce.hasLibraryContent())
				return false;
		}
		selection = elements;
		return true;
	}

	@Override
	public void run() {
		CopyPaste.copy(selection);
	}

	@Override
	public String getText() {
		return M.Copy;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.COPY.descriptor();
	}

}
