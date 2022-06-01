package org.openlca.app.db;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.openlca.app.collaboration.api.RepositoryClient;
import org.openlca.app.collaboration.util.WebRequests.WebRequestException;
import org.openlca.app.rcp.Workspace;
import org.openlca.core.database.IDatabase;
import org.openlca.git.GitConfig;
import org.openlca.git.ObjectIdStore;
import org.openlca.git.find.Commits;
import org.openlca.git.find.Datasets;
import org.openlca.git.find.Entries;
import org.openlca.git.find.Ids;
import org.openlca.git.find.References;
import org.openlca.git.util.Constants;
import org.openlca.git.util.History;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Repository {

	private static final Logger log = LoggerFactory.getLogger(Repository.class);
	private static Repository repository;
	public static final String GIT_DIR = "repositories";
	public final FileRepository git;
	public final String serverUrl;
	public final String repositoryId;
	public final RepositoryClient client;
	public final ObjectIdStore workspaceIds;
	public final Commits commits;
	public final Datasets datasets;
	public final Ids ids;
	public final References references;
	public final Entries entries;
	public final History history;
	private String password;

	private Repository(IDatabase database, File gitDir) throws IOException {
		git = new FileRepository(gitDir);
		var url = url(git);
		if (url.startsWith("git@")) {
			var splitIndex = url.lastIndexOf(":");
			serverUrl = url.substring(0, splitIndex);
			repositoryId = url.substring(splitIndex + 1);
		} else if (url.startsWith("http")) {
			var splitIndex = url.substring(0, url.lastIndexOf("/")).lastIndexOf("/");
			serverUrl = url.substring(0, splitIndex);
			repositoryId = url.substring(splitIndex + 1);
		} else {
			throw new IllegalArgumentException("Unsupported protocol");
		}
		client = new RepositoryClient(serverUrl, repositoryId);
		var storeFile = new File(gitDir, "object-id.store");
		workspaceIds = ObjectIdStore.open(storeFile);
		commits = Commits.of(git);
		datasets = Datasets.of(git);
		references = References.of(git);
		entries = Entries.of(git);
		ids = Ids.of(git);
		history = History.of(git);
	}

	public static Repository get() {
		return repository;
	}

	public static Repository open(IDatabase database) {
		close();
		var gitDir = gitDir(database.getName());
		if (!gitDir.exists() || !gitDir.isDirectory() || gitDir.listFiles().length == 0)
			return null;
		try {
			repository = new Repository(database, gitDir);
			return repository;
		} catch (IOException e) {
			log.error("Error opening Git repo", e);
			return null;
		}
	}

	public static Repository initialize(IDatabase database) {
		open(database);
		if (repository == null)
			return null;
		try {
			repository.isCollaborationServer(repository.client.isCollaborationServer());
		} catch (WebRequestException e) {
			repository.isCollaborationServer(false);
		}
		return repository;
	}

	public static File gitDir(String databaseName) {
		var repos = new File(Workspace.root(), GIT_DIR);
		return new File(repos, databaseName);
	}

	public static boolean isConnected() {
		return repository != null;
	}

	public static void close() {
		if (repository == null)
			return;
		repository.client.close();
		repository.git.close();
		repository = null;
	}

	private static String url(FileRepository repo) {
		try (var git = new Git(repo)) {
			var configs = git.remoteList().call();
			var config = configs.stream()
					.filter(c -> c.getName().equals(Constants.DEFAULT_REMOTE))
					.findFirst()
					.orElse(null);
			if (config == null || config.getURIs().isEmpty())
				throw new IllegalStateException("No remote URI configured");
			var uri = config.getURIs().get(0);
			return uri.toString();
		} catch (GitAPIException e) {
			log.error("Error reading Git config", e);
			return null;
		}
	}

	public GitConfig toConfig() {
		return new GitConfig(Database.get(), Repository.get().workspaceIds, Repository.get().git);
	}

	public String user() {
		return git.getConfig().getString("user", null, "name");
	}

	public void setUser(String user) {
		if (!user.equals(user())) {
			useTwoFactorAuth(false);
		}
		git.getConfig().setString("user", null, "name", user);
		saveConfig();
	}

	public String password() {
		return password;
	}

	public void password(String password) {
		this.password = password;
	}

	public void invalidateCredentials() {
		this.password = null;
	}

	public boolean useTwoFactorAuth() {
		return git.getConfig().getBoolean("user", null, "useTwoFactorAuth", false);
	}

	public void useTwoFactorAuth(boolean value) {
		git.getConfig().setString("user", null, "useTwoFactorAuth", Boolean.toString(value));
		saveConfig();
	}

	public boolean isCollaborationServer() {
		return git.getConfig().getBoolean("remote", "origin", "isCollaborationServer", false);
	}

	public void isCollaborationServer(boolean value) {
		git.getConfig().setBoolean("remote", "origin", "isCollaborationServer", value);
		saveConfig();
	}

	private void saveConfig() {
		try {
			git.getConfig().save();
		} catch (IOException e) {
			log.error("Error saving Git config", e);
		}
	}

}
