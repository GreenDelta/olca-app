package org.openlca.app.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.openlca.app.M;
import org.openlca.app.navigation.CopyPaste;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;

class PasteAction extends Action implements INavigationAction {

	private INavigationElement<?> category;

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		var first = selection.get(0);
		if (CopyPaste.cacheIsEmpty())
			return false;
		if (!CopyPaste.canPasteTo(first))
			return false;
		category = first;
		return true;
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
