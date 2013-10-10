package org.openlca.app.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.openlca.app.Messages;
import org.openlca.app.db.DatabaseWizard;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.resources.ImageType;

/**
 * Opens the wizard for creating a new database.
 */
public class DatabaseCreateAction extends Action implements INavigationAction {

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

	@Override
	public ImageDescriptor getImageDescriptor() {
		return ImageType.NEW_DB_ICON.getDescriptor();
	}

	@Override
	public String getText() {
		return Messages.CreateDatabaseAction;
	}

}
