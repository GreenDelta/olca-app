package org.openlca.app.collaboration.navigation.actions;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.ui.PlatformUI;
import org.openlca.app.collaboration.dialogs.AuthenticationDialog;
import org.openlca.app.collaboration.dialogs.AuthenticationDialog.GitCredentialsProvider;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.util.Question;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Version;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.git.actions.GitFetch;
import org.openlca.git.actions.GitInit;
import org.openlca.git.model.Commit;
import org.openlca.git.model.Entry.EntryType;
import org.openlca.git.util.Constants;
import org.openlca.git.util.TypeRefIdMap;
import org.openlca.jsonld.Json;
import org.openlca.util.Dirs;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

public class RepositoryUpgrade {

	private static final Logger log = LoggerFactory.getLogger(RepositoryUpgrade.class);
	private final IDatabase database;

	private RepositoryUpgrade(IDatabase database) {
		this.database = database;
	}

	public static void on(IDatabase database) {
		try {
			var upgrade = new RepositoryUpgrade(database);
			var config = upgrade.init();
			if (config == null)
				return;
			if (!Question.ask("Update repository connection",
					"You were previously connected to a Collaboration Server version 1. openLCA 2 requires Collaboration Server version 2. Do you want to update your server connection? (This requires that the server you are connected to is already updated)"))
				return;
			upgrade.run(config);
		} catch (Throwable e) {
			log.warn("Could not upgrade repository connection", e);
		}
	}

	void run(Config config) {
		var repo = initGit(config);
		if (repo == null)
			return;
		var credentials = AuthenticationDialog.promptCredentials(repo);
		if (credentials == null)
			return;
		var headCommit = fetch(repo, credentials);
		if (headCommit == null)
			return;
		updateHead(repo, headCommit);
		initObjectIds(repo, headCommit);
	}

	private Config init() {
		var dbDir = Workspace.dataDir().getDatabaseDir(database.getName());
		var oldDir = new File(dbDir, "_olca_/cloud");
		if (!oldDir.exists())
			return null;
		var config = getActiveConfig(oldDir);
		try {
			Dirs.delete(oldDir);
		} catch (Throwable e) {
			log.warn("Could not delete old repository index", e);
		}
		return config;
	}

	private Config getActiveConfig(File configsDir) {
		var files = configsDir.listFiles();
		if (files == null)
			return null;
		for (var dir : files) {
			if (!dir.isDirectory())
				continue;
			var configFile = new File(dir, "config.json");
			if (!configFile.exists())
				continue;
			try (var reader = new FileReader(configFile)) {
				var json = new Gson().fromJson(reader, JsonObject.class);
				var baseUrl = Json.getString(json, "baseUrl");
				var repositoryId = Json.getString(json, "repositoryId");
				var username = Json.getString(json, "username");
				if (!Json.getBool(json, "active", false)
						|| Strings.nullOrEmpty(baseUrl)
						|| Strings.nullOrEmpty(repositoryId)
						|| Strings.nullOrEmpty(username)
						|| !baseUrl.endsWith("/ws"))
					continue;
				var url = baseUrl.substring(0, baseUrl.length() - 3) + "/" + repositoryId;
				return new Config(url, username);
			} catch (IOException | JsonSyntaxException e) {
				log.warn("Error reading config file " + configFile.getAbsolutePath(), e);
			}
		}
		return null;
	}

	private Repository initGit(Config config) {
		try {
			var gitDir = Repository.gitDir(database.getName());
			GitInit.in(gitDir).remoteUrl(config.url).run();
			var repo = Repository.initialize(Database.get());
			repo.user(config.username);
			return repo;
		} catch (GitAPIException | URISyntaxException e) {
			log.warn("Error initializing git repo from " + config.url, e);
			return null;
		}
	}

	private Commit fetch(Repository repo, GitCredentialsProvider credentials) {
		try {
			var commits = Actions.run(credentials, GitFetch.to(repo.git));
			if (commits == null || commits.isEmpty())
				return null;
			return commits.get(0);
		} catch (GitAPIException | InvocationTargetException | InterruptedException e) {
			log.warn("Error fetching from " + repo.client.serverUrl + "/" + repo.client.repositoryId, e);
			return null;
		}
	}

	private void updateHead(Repository repo, Commit commit) {
		try {
			var update = repo.git.updateRef(Constants.LOCAL_BRANCH);
			update.setNewObjectId(ObjectId.fromString(commit.id));
			update.update();
		} catch (IOException e) {
			log.warn("Error updating head of " + repo.client.repositoryId, e);
		}
	}

	private void initObjectIds(Repository repo, Commit commit) {
		var service = PlatformUI.getWorkbench().getProgressService();
		try {
			service.run(true, false, monitor -> {
				monitor.beginTask("Initializing object ids", IProgressMonitor.UNKNOWN);
				repo.workspaceIds.putRoot(ObjectId.fromString(commit.id));
				initObjectIds(monitor, repo, commit, "");
				try {
					repo.workspaceIds.save();
				} catch (IOException e) {
					log.error("Error saving workspace object ids", e);
				}
				monitor.done();
			});
		} catch (InvocationTargetException | InterruptedException e) {
			log.error("Error saving workspace object ids", e);
		}
	}

	private void initObjectIds(IProgressMonitor monitor, Repository repo, Commit commit, String path) {
		var ids = repo.workspaceIds;
		var entries = repo.entries.find().commit(commit.id).path(path).all();
		var descriptors = new TypeRefIdMap<RootDescriptor>();
		for (var type : ModelType.values()) {
			database.getDescriptors(type.getModelClass()).forEach(
					descriptor -> descriptors.put(type, descriptor.refId, (RootDescriptor) descriptor));
		}
		entries.forEach(entry -> {
			ids.put(entry.path, entry.objectId);
			if (entry.typeOfEntry != EntryType.DATASET) {
				initObjectIds(monitor, repo, commit, entry.path);
			} else {
				var localModel = descriptors.get(entry.type, entry.refId);
				if (localModel != null) {
					monitor.subTask(entry.category + "/" + localModel.name);
					var remoteModel = repo.datasets.parse(entry, "lastChange", "version");
					var version = Version.fromString(string(remoteModel, "version")).getValue();
					var lastChange = number(remoteModel, "lastChange");
					if (version != localModel.version || lastChange != localModel.lastChange) {
						ids.invalidate(entry.path);
					}
				}
			}
		});
	}

	private String string(Map<String, Object> map, String field) {
		var value = map.get(field);
		if (value == null)
			return null;
		return value.toString();
	}

	private long number(Map<String, Object> map, String field) {
		var value = map.get(field);
		if (value == null)
			return 0;
		return Long.parseLong(value.toString());
	}

	private record Config(String url, String username) {
	}

}
