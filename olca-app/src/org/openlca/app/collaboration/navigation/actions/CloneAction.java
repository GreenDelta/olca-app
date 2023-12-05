package org.openlca.app.collaboration.navigation.actions;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.openlca.app.collaboration.dialogs.ConnectDialog;
import org.openlca.app.collaboration.util.Announcements;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.db.DatabaseDir;
import org.openlca.app.db.DbTemplate;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Input;
import org.openlca.core.database.config.DerbyConfig;
import org.openlca.core.database.upgrades.Upgrades;
import org.openlca.git.actions.GitInit;
import org.openlca.util.Dirs;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloneAction extends Action implements INavigationAction {

	private static final Logger log = LoggerFactory.getLogger(CloneAction.class);
	private final boolean standalone;

	private CloneAction(boolean standalone) {
		this.standalone = standalone;
	}

	public static CloneAction forImportMenu() {
		return new CloneAction(false);
	}

	public static CloneAction standalone() {
		return new CloneAction(true);
	}

	@Override
	public String getText() {
		return standalone ? "Import from Git..." : "From Git...";
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.CLONE.descriptor();
	}

	@Override
	public void run() {
		var dialog = new ConnectDialog().withPassword();
		if (dialog.open() == ConnectDialog.CANCEL)
			return;
		var url = dialog.url();
		var repoName = url.substring(url.lastIndexOf("/") + 1);
		var config = initDatabase(repoName);
		try {
			if (!initRepository(config.name(), dialog)) {
				onError(config);
				return;
			}
			new PullAction().run();
			Announcements.check();
		} catch (Exception e) {
			onError(config);
			Actions.handleException("Error importing repository", e);
		} finally {
			Cache.evictAll();
			Actions.refresh();
		}
	}

	private DerbyConfig initDatabase(String name) {
		var dbDir = getDbDir(name);
		if (dbDir == null)
			return null;
		var config = new DerbyConfig();
		config.name(dbDir.getName());
		DbTemplate.EMPTY.extract(dbDir);
		var db = Database.activate(config);
		if (db == null)
			return null;
		Upgrades.on(db);
		Database.register((DerbyConfig) config);
		return config;
	}

	private boolean initRepository(String dbName, ConnectDialog dialog)
			throws GitAPIException, URISyntaxException {
		var gitDir = Repository.gitDir(dbName);
		GitInit.in(gitDir).remoteUrl(dialog.url()).run();
		var repo = Repository.initialize(gitDir);
		if (repo == null)
			return false;
		repo.user(dialog.user());
		repo.password(dialog.credentials().password);
		return true;
	}

	private void onError(DerbyConfig config) {
		try {
			Database.close();
			if (config != null) {
				Database.remove(config);
				Dirs.delete(Repository.gitDir(config.name()));
				Dirs.delete(DatabaseDir.getRootFolder(config.name()));
			}
		} catch (Exception e1) {
			log.error("Error deleting unused files", e1);
		}
	}

	private File getDbDir(String name) {
		var dir = DatabaseDir.getRootFolder(name);
		if (!dir.exists())
			return dir;
		name = Input.prompt("Import a Git repository",
				"Please enter a name for the database",
				name,
				v -> v,
				Database::validateNewName);
		if (Strings.nullOrEmpty(name))
			return null;
		return getDbDir(name);
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.size() > 1)
			return false;
		if (selection.size() == 0)
			return standalone;
		var first = selection.get(0);
		if (!(first instanceof DatabaseElement))
			return false;
		var elem = (DatabaseElement) first;
		if (!Database.isActive(elem.getContent()))
			return standalone;
		return false;
	}

}
