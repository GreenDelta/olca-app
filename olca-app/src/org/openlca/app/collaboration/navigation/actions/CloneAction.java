package org.openlca.app.collaboration.navigation.actions;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.openlca.app.M;
import org.openlca.app.collaboration.dialogs.AuthenticationDialog;
import org.openlca.app.collaboration.dialogs.ConnectDialog;
import org.openlca.app.collaboration.navigation.elements.RepositoryElement;
import org.openlca.app.collaboration.util.Announcements;
import org.openlca.app.collaboration.util.CredentialStore;
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
	private final CloneActionType type;
	private RepositoryElement elem;

	private CloneAction(CloneActionType type) {
		this.type = type;
	}

	public static CloneAction forImportMenu() {
		return new CloneAction(CloneActionType.IMPORT);
	}

	public static CloneAction forRootMenu() {
		return new CloneAction(CloneActionType.ROOT);
	}

	public static CloneAction forRepositoryMenu() {
		return new CloneAction(CloneActionType.REPOSITORY);
	}

	@Override
	public String getText() {
		return switch (type) {
			case ROOT -> M.ImportFromGitDots;
			case IMPORT -> M.FromGitDots;
			case REPOSITORY -> M.Clone;
		};
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.CLONE.descriptor();
	}

	@Override
	public void run() {
		if (elem == null) {
			var dialog = new ConnectDialog().withPassword();
			if (dialog.open() == ConnectDialog.CANCEL)
				return;
			var url = dialog.url();
			clone(url, dialog.user(), dialog.password());
		} else {
			var serverUrl = elem.getServer().url;
			var url = serverUrl + "/" + elem.getRepositoryId();
			var credentials = AuthenticationDialog.promptCredentials(serverUrl);
			if (credentials == null)
				return;
			clone(url, credentials.user, credentials.password);
		}
	}

	private void clone(String url, String user, String password) {
		var repoName = url.substring(url.lastIndexOf("/") + 1);
		var config = initDatabase(repoName);
		if (config == null)
			return;
		try {
			if (!initRepository(config.name(), url, user, password)) {
				onError(config);
				return;
			}
			PullAction.silent().run();
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
		Database.register(config);
		return config;
	}

	private boolean initRepository(String dbName, String url, String user, String password)
			throws GitAPIException, URISyntaxException {
		var gitDir = Repository.gitDir(dbName);
		GitInit.in(gitDir).remoteUrl(url).run();
		var repo = Repository.initialize(gitDir, Database.get());
		if (repo == null)
			return false;
		repo.user(user);
		CredentialStore.put(url, user, password);
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
		name = Input.prompt(M.ImportGitRepository,
				M.PleaseEnterADatabaseName,
				name,
				v -> v,
				Database::validateNewName);
		if (Strings.nullOrEmpty(name))
			return null;
		return getDbDir(name);
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		this.elem = null;
		if (selection.size() > 1)
			return false;
		if (selection.size() == 0)
			return type == CloneActionType.ROOT;
		var first = selection.get(0);
		if (first instanceof DatabaseElement dbElem)
			return !Database.isActive(dbElem.getContent()) && type == CloneActionType.ROOT;
		if (first instanceof RepositoryElement repoElem && type == CloneActionType.REPOSITORY) {
			this.elem = repoElem;
			return true;
		}
		return false;
	}

	private enum CloneActionType {

		ROOT, IMPORT, REPOSITORY;

	}

}
