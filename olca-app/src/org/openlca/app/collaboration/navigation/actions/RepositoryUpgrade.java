package org.openlca.app.collaboration.navigation.actions;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Map;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.openlca.app.collaboration.dialogs.AuthenticationDialog.GitCredentialsProvider;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.util.Input;
import org.openlca.app.util.Question;
import org.openlca.core.database.CategoryDao;
import org.openlca.core.database.Daos;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Version;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.git.actions.ConflictResolver;
import org.openlca.git.actions.GitFetch;
import org.openlca.git.actions.GitInit;
import org.openlca.git.actions.GitMerge;
import org.openlca.git.actions.GitStashCreate;
import org.openlca.git.model.Change;
import org.openlca.git.model.Commit;
import org.openlca.git.model.Diff;
import org.openlca.git.model.ModelRef;
import org.openlca.git.util.Constants;
import org.openlca.git.util.ModelRefMap;
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
			if (!Question.ask("Update repository connection", """
					You were previously connected to a Collaboration Server version 1.
					openLCA 2 requires Collaboration Server version 2. Do you want to
					update your server connection? (This requires that the server you are
					connected to is already updated; if that is not the case you will be
					prompted for the URL of the new server)"""))
				return;
			if (!upgrade.run(config)) {
				Dirs.delete(Repository.gitDir(database.getName()));
			}
		} catch (Throwable e) {
			log.warn("Could not upgrade repository connection", e);
		}
	}

	boolean run(Config config) {
		var repo = initGit(config.url, config.username);
		if (repo == null)
			return false;
		var credentials = repo.promptCredentials();
		if (credentials == null)
			return false;
		return pull(repo, credentials);
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

	private Repository initGit(String url, String user) {
		try {
			var gitDir = Repository.gitDir(database.getName());
			if (gitDir.exists() && gitDir.list() != null && gitDir.list().length > 0) {
				Dirs.delete(gitDir);
			}
			GitInit.in(gitDir).remoteUrl(url).run();
			var repo = Repository.initialize(gitDir, Database.get());
			if (repo == null)
				return null;
			if (repo.isCollaborationServer()) {
				repo.user(user);
				return repo;
			}
			url = Input.promptString("Could not connect",
					"Could not connect, this might be an older version of the Collaboration Server? Please specify the url to the updated repository:",
					url);
			if (url == null)
				return null;
			return initGit(url, user);
		} catch (GitAPIException | URISyntaxException e) {
			log.warn("Error initializing git repo from " + url, e);
			return null;
		}
	}

	private boolean pull(Repository repo, GitCredentialsProvider credentials) {
		try {
			var commits = Actions.run(credentials, GitFetch.to(repo));
			if (commits == null || commits.isEmpty())
				return true;
			var libraryResolver = WorkspaceLibraryResolver.forRemote();
			if (libraryResolver == null)
				return false;
			var descriptors = new ModelRefMap<RootDescriptor>();
			for (var type : ModelType.values()) {
				if (type == ModelType.CATEGORY) {
					new CategoryDao(Database.get()).getAll().forEach(
							c -> descriptors.put(c.modelType, c.modelType.name() + "/" + c.toPath(), Descriptor.of(c)));
				} else {
					Daos.root(Database.get(), type).getDescriptors().forEach(d -> descriptors.put(d.type, d.refId, d));
				}
			}
			var commit = repo.commits.find().refs(Constants.REMOTE_REF).latest();
			boolean wasStashed = stashDifferences(repo, commit, credentials.ident, descriptors);
			Actions.run(GitMerge.on(repo)
					.as(credentials.ident)
					.resolveLibrariesWith(libraryResolver)
					.resolveConflictsWith(new EqualResolver(descriptors)));
			if (!wasStashed)
				return true;
			return Actions.applyStash();
		} catch (GitAPIException | InvocationTargetException | InterruptedException | IOException e) {
			log.warn("Error pulling from " + repo.url + "/" + repo.id, e);
			return false;
		}
	}

	private boolean stashDifferences(Repository repo, Commit commit, PersonIdent user,
			ModelRefMap<RootDescriptor> descriptors)
			throws IOException, InvocationTargetException, InterruptedException, GitAPIException {
		var differences = Change.of(
				repo.diffs.find().commit(commit).withDatabase().stream()
						.filter(diff -> !equalsDescriptor(diff, descriptors.get(diff)))
						.toList());
		if (differences.isEmpty())
			return false;
		Actions.run(GitStashCreate.on(repo)
				.as(user)
				.parent(commit)
				.changes(differences));
		return true;
	}

	private static boolean equalsDescriptor(Diff diff, RootDescriptor d) {
		if (d == null)
			return false;
		if (diff.oldRef == null || ObjectId.zeroId().equals(diff.oldRef.objectId))
			return false;
		if (diff.isCategory)
			return true;
		var remoteModel = Repository.CURRENT.datasets.parse(diff.oldRef, "lastChange", "version");
		if (remoteModel == null)
			return false;
		var version = Version.fromString(string(remoteModel, "version")).getValue();
		var lastChange = date(remoteModel, "lastChange");
		return version == d.version && lastChange == d.lastChange;
	}

	private static String string(Map<String, Object> map, String field) {
		var value = map.get(field);
		if (value == null)
			return null;
		return value.toString();
	}

	private static long date(Map<String, Object> map, String field) {
		var value = map.get(field);
		if (value == null)
			return 0;
		try {
			return Long.parseLong(value.toString());
		} catch (NumberFormatException e) {
			var date = Json.parseDate(value.toString());
			if (date == null)
				return 0;
			return date.getTime();
		}
	}

	private record Config(String url, String username) {
	}

	private class EqualResolver implements ConflictResolver {

		private final ModelRefMap<RootDescriptor> descriptors;

		private EqualResolver(ModelRefMap<RootDescriptor> descriptors) {
			this.descriptors = descriptors;
		}

		@Override
		public boolean isConflict(ModelRef ref) {
			return descriptors.contains(ref);
		}

		@Override
		public ConflictResolutionType peekConflictResolution(ModelRef ref) {
			return isConflict(ref) ? ConflictResolutionType.IS_EQUAL : null;
		}

		@Override
		public ConflictResolution resolveConflict(ModelRef ref, JsonObject fromHistory) {
			return isConflict(ref) ? ConflictResolution.isEqual() : null;
		}

	}

}
