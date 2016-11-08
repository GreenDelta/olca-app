package org.openlca.app.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.openlca.app.M;
import org.openlca.app.navigation.CopyPaste;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.rcp.images.Icon;

class PasteAction extends Action implements INavigationAction {

	private INavigationElement<?> category;

	@Override
	public boolean accept(INavigationElement<?> element) {
		if (CopyPaste.cacheIsEmpty())
			return false;
		if (!CopyPaste.canPasteTo(element))
			return false;
		category = element;
		return true;
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return false;
	}

	@Override
	public void run() {
		CopyPaste.pasteTo(category);
	}

	@Override
	public String getText() {
		return M.Paste;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.PASTE.descriptor();
	}

}
