package org.openlca.app.collaboration.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.openlca.app.M;
import org.openlca.app.collaboration.Repository;
import org.openlca.app.collaboration.dialogs.ConnectDialog;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.MsgBox;

public class AddDataPackageAction extends Action implements INavigationAction {

	@Override
	public String getText() {
		return M.AddDataPackageDots;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.LIBRARY.descriptor();
	}

	@Override
	public void run() {
		var dialog = new ConnectDialog();
		if (dialog.open() == ConnectDialog.CANCEL)
			return;
		var url = dialog.url();
		var packageName = url.substring(url.lastIndexOf("/") + 1);
		var db = Database.get();
		if (db.getDataPackage(packageName) != null) {
			MsgBox.warning("Data package already exists");
			return;
		}
		var dataPackage = Database.get().addDataPackage(packageName, url);
		try {
			var repo = Repository.initialize(Database.get(), dataPackage, url);
			if (repo == null)
				return;
			repo.user(dialog.user());
			PullAction.silent().on(repo).run();
			repo.close();
		} catch (Exception e) {
			Actions.handleException("Error connecting to repository", url, e);
		} finally {
			Actions.refresh();
		}
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		var first = selection.get(0);
		if (!(first instanceof DatabaseElement))
			return false;
		var elem = (DatabaseElement) first;
		return Database.isActive(elem.getContent());
	}

}
