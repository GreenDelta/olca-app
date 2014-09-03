package org.openlca.app.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.Messages;
import org.openlca.app.navigation.CopyPaste;
import org.openlca.app.navigation.INavigationElement;

public class PasteAction extends Action implements INavigationAction {

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
		return Messages.Paste;
	}

}
