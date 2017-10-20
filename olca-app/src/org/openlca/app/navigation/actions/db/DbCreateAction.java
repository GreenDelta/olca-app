package org.openlca.app.navigation.actions.db;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.M;
import org.openlca.app.db.DatabaseWizard;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.rcp.images.Images;

/**
 * Opens the wizard for creating a new database.
 */
public class DbCreateAction extends Action implements INavigationAction {

	public DbCreateAction() {
		setText(M.NewDatabase);
		setImageDescriptor(Images.newDatabase());
	}

	@Override
	public boolean accept(INavigationElement<?> element) {
		return false;
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return false;
	}

	@Override
	public void run() {
		DatabaseWizard.open();
	}

}
