package org.openlca.app.navigation.actions.db;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.db.DatabasePropertiesDialog;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.core.database.config.DatabaseConfig;

/**
 * Shows the database properties in a window.
 */
public class DbPropertiesAction extends Action implements
		INavigationAction {

	private DatabaseConfig config;

	public DbPropertiesAction() {
		setText(M.Properties);
		setImageDescriptor(Icon.INFO.descriptor());
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		var first = selection.get(0);
		if (!(first instanceof DatabaseElement))
			return false;
		var dbElement = (DatabaseElement) first;
		config = dbElement.getContent();
		return true;
	}

	@Override
	public void run() {
		if (config == null) {
			config = Database.getActiveConfiguration();
			if (config == null)
				return;
		}
		new DatabasePropertiesDialog(config).open();
	}
}
