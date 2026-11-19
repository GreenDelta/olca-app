package org.openlca.app.navigation.actions.db;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.osgi.util.NLS;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.collaboration.Repository;
import org.openlca.app.collaboration.views.CompareView;
import org.openlca.app.collaboration.views.HistoryView;
import org.openlca.app.db.Database;
import org.openlca.app.db.DatabaseDir;
import org.openlca.app.editors.Editors;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Question;
import org.openlca.commons.Res;
import org.openlca.core.database.config.DatabaseConfig;
import org.openlca.core.database.config.DerbyConfig;
import org.openlca.core.database.config.MySqlConfig;
import org.openlca.util.Dirs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Deletes a database. Works only for local Derby databases; remote databases
/// cannot be deleted.
public class DbDeleteAction extends Action implements INavigationAction {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final List<DatabaseConfig> configs = new ArrayList<>();

	public DbDeleteAction() {
		setImageDescriptor(Icon.DELETE.descriptor());
		setDisabledImageDescriptor(Icon.DELETE_DISABLED.descriptor());
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		configs.clear();
		if (selection.isEmpty()) {
			var config = Database.getActiveConfiguration();
			if (config == null)
				return false;
			configs.add(config);
		} else {
			selection.stream()
					.filter(e -> e instanceof DatabaseElement)
					.map(e -> ((DatabaseElement) e).getContent())
					.forEach(configs::add);
		}
		return !configs.isEmpty();
	}

	@Override
	public String getText() {
		return M.DeleteDatabase;
	}

	@Override
	public void run() {
		if (configs.isEmpty())
			return;
		var name = configs.size() == 1
				? configs.getFirst().name()
				: M.TheSelectedDatabases;
		if (!Question.askDelete(name))
			return;
		if (!checkCloseEditors())
			return;
		App.exec(M.DeleteDatabase, this::doDelete, () -> {
			Navigator.refresh();
			HistoryView.refresh();
			CompareView.clear();
		});
	}

	private boolean checkCloseEditors() {
		for (DatabaseConfig config : this.configs)
			if (Database.isActive(config))
				return Editors.closeAll();
		return true;
	}

	private void doDelete() {
		// remove the leftovers of previous deletions, if there are any
		DatabaseDir.deleteTrash();

		var errors = new ArrayList<String>();
		for (var config : this.configs) {
			try {
				var res = tryDelete(config);
				if (res.isError()) {
					errors.add(res.error());
				}
			} catch (Exception e) {
				ErrorReporter.on("Failed to delete database " + config.name(), e);
			}
		}
		if (!errors.isEmpty()) {
			MsgBox.error(M.FailedToDeleteDatabase, String.join("\n\n", errors));
		}
	}

	/// Deletes the given database. To not leave an incomplete database behind
	/// when its folder cannot be deleted, the folder is first renamed to a trash
	/// folder. If this fails, nothing was changed and an error is returned.
	/// Otherwise, the database is removed from the configurations and the trash
	/// folder is deleted; if this deletion fails, the folder is deleted later
	/// (see `DatabaseDir.deleteTrash`).
	private Res<Void> tryDelete(DatabaseConfig config) throws Exception {
		if (Database.isActive(config)) {
			Database.close();
		}

		File trash = null;
		var dbFolder = DatabaseDir.getRootFolder(config.name());
		if (dbFolder.isDirectory()) {
			var res = DatabaseDir.moveToTrash(dbFolder);
			if (res.isError()) {
				log.warn("could not delete database {}: {}", config.name(), res.error());
				return Res.error(NLS.bind(
					M.DatabaseFolderInUse, config.name(), dbFolder.getPath()));
			}
			trash = res.value();
		}

		switch (config) {
			case DerbyConfig derby -> Database.remove(derby);
			case MySqlConfig mySql -> Database.remove(mySql);
			default -> {
			}
		}
		deleteRepository(config);
		if (trash != null) {
			deleteTrash(trash);
		}
		return Res.ok();
	}

	/// Deletes the repository of the database, if there is one. If this fails,
	/// `Repository.delete` shows a warning to the user.
	private void deleteRepository(DatabaseConfig config) {
		try {
			Repository.delete(config.name());
		} catch (Exception e) {
			log.warn("failed to delete repository of database {}", config.name(), e);
		}
	}

	private void deleteTrash(File trash) {
		try {
			Dirs.delete(trash);
		} catch (Exception e) {
			log.warn("failed to delete database trash folder {}; "
				+ "it will be deleted later", trash, e);
		}
	}

}
