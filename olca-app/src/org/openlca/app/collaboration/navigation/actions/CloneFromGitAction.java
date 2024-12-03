package org.openlca.app.collaboration.navigation.actions;


import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.M;
import org.openlca.app.collaboration.dialogs.ConnectDialog;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.DatabaseDirElement;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.NavigationRoot;
import org.openlca.app.rcp.images.Icon;

public class CloneFromGitAction extends Action implements INavigationAction {

	public CloneFromGitAction() {
		setText(M.FromRepositoryDots);
		setImageDescriptor(Icon.CLONE.descriptor());
	}

	@Override
	public void run() {
		var dialog = new ConnectDialog();
		if (dialog.open() == ConnectDialog.CANCEL)
			return;
		Clone.of(dialog.url(), dialog.user(), dialog.password());
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.isEmpty())
			return true;
		var first = selection.get(0);
		return first instanceof DatabaseElement
				|| first instanceof DatabaseDirElement
				|| first instanceof NavigationRoot;
	}

}
