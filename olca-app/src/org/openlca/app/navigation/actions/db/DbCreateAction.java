package org.openlca.app.navigation.actions.db;

import org.eclipse.jface.action.Action;
import org.openlca.app.M;
import org.openlca.app.db.DatabaseWizard;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.DatabaseDirElement;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.NavigationRoot;
import org.openlca.app.rcp.images.Images;
import org.openlca.util.Strings;

import java.util.List;

/**
 * Opens the wizard for creating a new database.
 */
public class DbCreateAction extends Action implements INavigationAction {

	private String folder;

	public DbCreateAction() {
		setText(M.NewDatabase);
		setImageDescriptor(Images.newDatabase());
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		folder = "";
		if (selection.isEmpty())
			return true;
		var first = selection.get(0);
		if (first instanceof DatabaseDirElement dir) {
			folder = String.join("/", dir.path());
			return true;
		}
		return first instanceof DatabaseElement
				|| first instanceof NavigationRoot;
	}

	@Override
	public void run() {
		if (Strings.notEmpty(folder)) {
			DatabaseWizard.open(folder);
		} else {
			DatabaseWizard.open();
		}
	}
}
