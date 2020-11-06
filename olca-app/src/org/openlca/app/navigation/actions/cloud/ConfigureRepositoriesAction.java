package org.openlca.app.navigation.actions.cloud;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.cloud.Announcements;
import org.openlca.app.cloud.index.Reindexing;
import org.openlca.app.cloud.ui.ConfigureRepositoriesDialog;
import org.openlca.app.cloud.ui.commits.HistoryView;
import org.openlca.app.cloud.ui.diff.CompareView;
import org.openlca.app.db.Database;
import org.openlca.app.editors.CommentsEditor;
import org.openlca.app.navigation.DatabaseElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.util.MsgBox;
import org.openlca.cloud.api.RepositoryClient;
import org.openlca.cloud.api.RepositoryConfig;

public class ConfigureRepositoriesAction extends Action implements INavigationAction {

	@Override
	public String getText() {
		return "#Configure...";
	}

	@Override
	public void run() {
		RepositoryConfig oldConfig = RepositoryConfig.loadActive(Database.get());
		ConfigureRepositoriesDialog dialog = new ConfigureRepositoriesDialog();
		dialog.open();
		RepositoryConfig config = RepositoryConfig.loadActive(Database.get());
		if (config == null && oldConfig != null) {
			CommentsEditor.close();
			Database.disconnect();
			refresh();
		}
		if (config == null || config.equals(oldConfig))
			return;
		connect(config);
		refresh();
	}

	private void connect(RepositoryConfig config) {
		RepositoryClient client = new RepositoryClient(config);
		try {
			if (!client.hasAccess(config.repositoryId)) {
				MsgBox.error(M.NoAccessToRepository);
				return;
			}
		} catch (Exception e) {
			MsgBox.error(e.getMessage());
			return;
		}
		Database.connect(client);
		Announcements.check(client);
		App.runWithProgress(M.RebuildingIndex, Reindexing::execute);
	}

	private void refresh() {
		Navigator.refresh(Navigator.getNavigationRoot());
		HistoryView.refresh();
		CompareView.clear();
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

	// private class Runner {
	//
	// private Exception error;
	//
	// private boolean run() {
	// AddRepositoryDialog dialog = new AddRepositoryDialog();
	// if (dialog.open() != Dialog.OK)
	// return false;
	// RepositoryConfig config = null;
	// RepositoryConfig.add(Database.get(), dialog.getBaseUrl(),
	// dialog.getRepositoryId(),
	// dialog.createCredentials());
	// config.activate();
	// String text = M.ConnectingToRepository + ": " + config.getServerUrl() + "
	// " + config.repositoryId;
	// App.runWithProgress(text, () -> connect(config));
	// HistoryView.refresh();
	// CompareView.clear();
	// return true;
	// }
	//
	// private void connect(RepositoryConfig config) {
	// RepositoryClient client = new RepositoryClient(config);
	// try {
	// if (!client.hasAccess(config.repositoryId)) {
	// error = new Exception(M.NoAccessToRepository);
	// }
	// } catch (Exception e) {
	// error = e;
	// }
	// if (error == null) {
	// Database.connect(client);
	// Announcements.check(client);
	// } else {
	// config.remove();
	// }
	// }
	//
	// }

}
