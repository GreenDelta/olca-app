package org.openlca.app.navigation.actions.db;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.tools.ValidationDialog;

public class DbValidationAction extends Action implements INavigationAction {

	public DbValidationAction() {
		setText(M.Validate);
		setImageDescriptor(Icon.VALIDATE.descriptor());
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.isEmpty())
			return false;
		var first = selection.get(0);
		if (!(first instanceof DatabaseElement))
			return false;
		var elem = (DatabaseElement) first;
		var config = elem.getContent();
		return Database.isActive(config);
	}

	@Override
	public void run() {
		ValidationDialog.show();
	}
}
