package org.openlca.app.navigation.actions.db;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jgit.storage.file.WindowCacheConfig;
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
import org.openlca.app.util.Question;
import org.openlca.core.database.config.DatabaseConfig;
import org.openlca.core.database.config.DerbyConfig;
import org.openlca.core.database.config.MySqlConfig;
import org.openlca.util.Dirs;

/**
 * Deletes a database. Works only for local Derby databases; remote databases
 * cannot be deleted.
 */
public class DbDeleteAction extends Action implements INavigationAction {

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
				? configs.get(0).name()
				: M.TheSelectedDatabases;
		if (!Question.askDelete(name))
			return;
		if (!checkCloseEditors())
			return;
		App.run(M.DeleteDatabase, this::doDelete, () -> {
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
		for (var config : this.configs) {
			try {
				tryDelete(config);
			} catch (Exception e) {
				ErrorReporter.on("failed to delete database", e);
			}
		}
	}

	private void tryDelete(DatabaseConfig config) throws Exception {
		if (Database.isActive(config))
			Database.close();
		File dbFolder = DatabaseDir.getRootFolder(config.name());
		if (dbFolder.isDirectory())
			Dirs.delete(dbFolder);
		// TODO this is a workaround to avoid open file handles that jgit
		// is holding (see https://github.com/eclipse-jgit/jgit/issues/155)
		new WindowCacheConfig().install();
		//
		Repository.delete(config.name());
		if (config instanceof DerbyConfig)
			Database.remove((DerbyConfig) config);
		else if (config instanceof MySqlConfig)
			Database.remove((MySqlConfig) config);
	}

}
