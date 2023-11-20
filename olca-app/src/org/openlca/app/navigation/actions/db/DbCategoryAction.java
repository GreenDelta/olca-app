package org.openlca.app.navigation.actions.db;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.DatabaseDirElement;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.UI;
import org.openlca.core.database.config.DerbyConfig;

import java.util.List;
import java.util.Objects;

public class DbCategoryAction extends Action implements INavigationAction {

	private DatabaseElement elem;
	private DerbyConfig config;

	public DbCategoryAction() {
		setText("Set folder");
		setImageDescriptor(Icon.FOLDER.descriptor());
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		if (!(selection.get(0) instanceof DatabaseElement e))
			return false;
		var config = e.getContent();
		if (!(config instanceof DerbyConfig c))
			return false;
		this.elem = e;
		this.config = c;
		return true;
	}

	@Override
	public void run() {
		if (elem == null
				|| config == null
				|| !Objects.equals(elem.getContent(), config))
			return;

		var path = DatabaseDirElement.split(config.category());
		var current = String.join("/", path);

		var dialog = new InputDialog(
				UI.shell(),
				"Set the database folder",
				"Please enter the path of the database folder. Forward slashes " +
						"are used as separators, e.g. my/database/folder.",
				current,
				null);
		if (dialog.open() != Window.OK)
			return;

		var nextPath = DatabaseDirElement.split(dialog.getValue());
		var next = String.join("/", nextPath);
		if (Objects.equals(current, next))
			return;
		config.setCategory(next);
		Database.saveConfig();
		Navigator.refresh();
	}
}
