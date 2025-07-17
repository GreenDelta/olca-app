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

public class AddRepositoryAction extends Action implements INavigationAction {

	@Override
	public String getText() {
		return M.AddSubRepositoryDots;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.REPOSITORY.descriptor();
	}
	


	public static void connect(String url, String user) {
		if (Repository.isConnected(url)) {
			MsgBox.warning("Data package already exists");
			return;
		}
		var packageName = url.substring(url.lastIndexOf("/") + 1);
		var dataPackage = Database.get().addRepository(packageName, null, url);
		try {
			var repo = Repository.initialize(Database.get(), dataPackage, url);
			if (repo == null)
				return;
			repo.user(user);
			PullAction.silent().on(repo).run();
		} catch (Exception e) {
			Actions.handleException("Error connecting to repository", url, e);
		} finally {
			Actions.refresh();
		}
	}

	@Override
	public void run() {
		var dialog = new ConnectDialog();
		if (dialog.open() == ConnectDialog.CANCEL)
			return;
		connect(dialog.url(), dialog.user());
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		var first = selection.get(0);
		if (!(first instanceof DatabaseElement elem))
			return false;
		return Database.isActive(elem.getContent());
	}

}
