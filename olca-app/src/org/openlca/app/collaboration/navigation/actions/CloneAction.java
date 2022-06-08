package org.openlca.app.collaboration.navigation.actions;

import java.io.File;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.openlca.app.collaboration.dialogs.ConnectDialog;
import org.openlca.app.collaboration.util.Announcements;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.db.DatabaseDir;
import org.openlca.app.db.DatabaseWizardPage;
import org.openlca.app.db.DbTemplate;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Input;
import org.openlca.core.database.config.DerbyConfig;
import org.openlca.core.database.upgrades.Upgrades;
import org.openlca.git.actions.GitFetch;
import org.openlca.git.actions.GitInit;
import org.openlca.git.actions.GitMerge;
import org.openlca.util.Dirs;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloneAction extends Action implements INavigationAction {

	private static final Logger log = LoggerFactory.getLogger(CloneAction.class);

	@Override
	public String getText() {
		return "From Git...";
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
		File dbDir = null;
		File gitDir = null;
		DerbyConfig config = null;
		try {
			var dbName = url.substring(url.lastIndexOf("/") + 1);
			dbDir = getDbDir(dbName);
			if (dbDir == null)
				return;
			config = new DerbyConfig();
			config.name(dbDir.getName());
			DbTemplate.EMPTY.extract(dbDir);
			var db = Database.activate(config);
			if (db == null)
				return;
			Upgrades.on(db);
			Database.register((DerbyConfig) config);
			gitDir = Repository.gitDir(db.getName());
			GitInit.in(gitDir).remoteUrl(url).run();
			var repo = Repository.initialize(db);
			repo.user(dialog.user());
			repo.password(dialog.credentials().password);
			var newCommits = Actions.run(dialog.credentials(),
					GitFetch.to(repo.git));
			if (newCommits == null || newCommits.isEmpty())
				return;
			var libraryResolver = WorkspaceLibraryResolver.forRemote();
			if (libraryResolver == null)
				return;
			Actions.run(GitMerge
					.from(repo.git)
					.into(db)
					.update(repo.workspaceIds)
					.resolveLibrariesWith(libraryResolver));
			Announcements.check();
		} catch (Exception e) {
			try {
				Database.close();
				if (config != null) {
					Database.remove(config);
				}
				Dirs.delete(gitDir);
				Dirs.delete(dbDir);
			} catch (Exception e1) {
				log.error("Error importing database", e1);
			}
			Actions.handleException("Error importing repository", e);
		} finally {
			Cache.evictAll();
			Actions.refresh();
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
				DatabaseWizardPage::validateName);
		if (Strings.nullOrEmpty(name))
			return null;
		return getDbDir(name);
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		var first = selection.get(0);
		if (!(first instanceof DatabaseElement))
			return false;
		var elem = (DatabaseElement) first;
		if (!Database.isActive(elem.getContent()))
			return false;
		return !Repository.isConnected();
	}

}
