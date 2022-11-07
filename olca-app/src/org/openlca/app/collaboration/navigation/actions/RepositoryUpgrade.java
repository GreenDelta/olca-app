package org.openlca.app.collaboration.navigation.actions;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.openlca.app.collaboration.dialogs.AuthenticationDialog;
import org.openlca.app.collaboration.dialogs.AuthenticationDialog.GitCredentialsProvider;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.util.Question;
import org.openlca.core.database.IDatabase;
import org.openlca.git.actions.GitFetch;
import org.openlca.git.actions.GitInit;
import org.openlca.git.actions.GitMerge;
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
		// TODO currently disabled
		// try {
		// var upgrade = new RepositoryUpgrade(database);
		// var config = upgrade.init();
		// if (config == null)
		// return;
		// if (!Question.ask("Update repository connection",
		// "You were previously connected to a Collaboration Server version 1.
		// openLCA 2 requires Collaboration Server version 2. Do you want to
		// update your server connection? (This requires that the server you are
		// connected to is already updated)"))
		// return;
		// upgrade.run(config);
		// } catch (Throwable e) {
		// log.warn("Could not upgrade repository connection", e);
		// }
	}

	void run(Config config) {
		var repo = initGit(config);
		if (repo == null)
			return;
		var credentials = AuthenticationDialog.promptCredentials(repo);
		if (credentials == null)
			return;
		pull(repo, credentials);
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
			if (gitDir.exists() && gitDir.list() != null && gitDir.list().length > 0) {
				Dirs.delete(gitDir);
			}
			GitInit.in(gitDir).remoteUrl(config.url).run();
			var repo = Repository.initialize(Database.get());
			repo.user(config.username);
			return repo;
		} catch (GitAPIException | URISyntaxException e) {
			log.warn("Error initializing git repo from " + config.url, e);
			return null;
		}
	}

	private void pull(Repository repo, GitCredentialsProvider credentials) {
		try {
			var commits = Actions.run(credentials, GitFetch.to(repo.git));
			if (commits == null || commits.isEmpty())
				return;
			Actions.run(GitMerge.from(repo.git)
					.into(database)
					.as(AuthenticationDialog.promptUser(repo))
					.resolveConflictsWith(null));
		} catch (GitAPIException | InvocationTargetException | InterruptedException | IOException e) {
			log.warn("Error pulling from " + repo.client.serverUrl + "/" + repo.client.repositoryId, e);
		}
	}

	private record Config(String url, String username) {
	}

}
