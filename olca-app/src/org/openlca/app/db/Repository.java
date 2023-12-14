package org.openlca.app.db;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.StoredConfig;
import org.openlca.app.collaboration.api.RepositoryClient;
import org.openlca.app.rcp.Workspace;
import org.openlca.git.GitIndex;
import org.openlca.git.find.Commits;
import org.openlca.git.find.Datasets;
import org.openlca.git.util.Constants;
import org.openlca.git.util.History;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Repository {

	private static final Logger log = LoggerFactory.getLogger(Repository.class);
	private static Repository current;
	public static final String GIT_DIR = "repositories";
	public final org.eclipse.jgit.lib.Repository git;
	public final RepositoryClient client;
	public final GitIndex gitIndex;
	public final Commits commits;
	public final Datasets datasets;
	public final History localHistory;
	private String password;

	private Repository(File gitDir) throws IOException {
		git = new FileRepository(gitDir);
		client = client(git);
		var storeFile = new File(gitDir, "git.index");
		gitIndex = GitIndex.fromFile(storeFile);
		commits = Commits.of(git);
		datasets = Datasets.of(git);
		localHistory = History.localOf(git);
	}

	public static Repository get() {
		return current;
	}

	public static void checkIfCollaborationServer() {
		checkIfCollaborationServer(current);
	}

	public static void checkIfCollaborationServer(FileRepository gitRepo) throws IOException {
		if (gitRepo == null)
			return;
		var client = client(gitRepo);
		isCollaborationServer(gitRepo.getConfig(), client != null && client.isCollaborationServer());
	}

	public static void checkIfCollaborationServer(Repository repo) {
		if (repo == null)
			return;
		repo.isCollaborationServer(repo.client != null && repo.client.isCollaborationServer());
	}

	public static Repository open(File gitDir) {
		close();
		if (!gitDir.exists() || !gitDir.isDirectory() || gitDir.listFiles().length == 0)
			return null;
		try {
			current = new Repository(gitDir);
			return current;
		} catch (IOException e) {
			log.error("Error opening Git repo", e);
			return null;
		}
	}

	public static Repository initialize(File gitDir) {
		var repo = open(gitDir);
		checkIfCollaborationServer(repo);
		return repo;
	}

	public static File gitDir(String databaseName) {
		var repos = new File(Workspace.root(), GIT_DIR);
		return new File(repos, databaseName);
	}

	public static RepositoryClient client(org.eclipse.jgit.lib.Repository git) throws IOException {
		var url = url(git);
		if (Strings.nullOrEmpty(url))
			return null;
		if (url.startsWith("git@")) {
			var splitIndex = url.lastIndexOf(":");
			var serverUrl = url.substring(0, splitIndex);
			var repositoryId = url.substring(splitIndex + 1);
			return new RepositoryClient(serverUrl, repositoryId);
		} else if (url.startsWith("http")) {
			var splitIndex = url.substring(0, url.lastIndexOf("/")).lastIndexOf("/");
			var serverUrl = url.substring(0, splitIndex);
			var repositoryId = url.substring(splitIndex + 1);
			return new RepositoryClient(serverUrl, repositoryId);
		}
		throw new IllegalArgumentException("Unsupported protocol");
	}

	public static boolean isConnected() {
		return current != null;
	}

	public static void close() {
		if (current == null)
			return;
		if (current.client != null) {
			current.client.close();
		}
		current.git.close();
		current = null;
	}

	public String url() {
		return url(git);
	}

	private static String url(org.eclipse.jgit.lib.Repository repo) {
		try (var git = new Git(repo)) {
			var configs = git.remoteList().call();
			var config = configs.stream()
					.filter(c -> c.getName().equals(Constants.DEFAULT_REMOTE))
					.findFirst()
					.orElse(null);
			if (config == null || config.getURIs().isEmpty())
				return null;
			var uri = config.getURIs().get(0);
			return uri.toString();
		} catch (GitAPIException e) {
			log.error("Error reading Git config", e);
			return null;
		}
	}

	public static String user(StoredConfig config) {
		return config.getString("user", null, "name");
	}

	public String user() {
		return user(git.getConfig());
	}

	public void user(String user) {
		if (!user.equals(user())) {
			useTwoFactorAuth(false);
		}
		var config = git.getConfig();
		config.setString("user", null, "name", user);
		saveConfig(config);
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
		var config = git.getConfig();
		config.setString("user", null, "useTwoFactorAuth", Boolean.toString(value));
		saveConfig(config);
	}

	public static boolean isCollaborationServer(StoredConfig config) {
		return config.getBoolean("remote", "origin", "isCollaborationServer", false);
	}

	public boolean isCollaborationServer() {
		return isCollaborationServer(git.getConfig()) && client != null;
	}

	public static void isCollaborationServer(StoredConfig config, boolean value) {
		config.setBoolean("remote", "origin", "isCollaborationServer", value);
		saveConfig(config);
	}

	public void isCollaborationServer(boolean value) {
		isCollaborationServer(git.getConfig(), value);
	}

	private static void saveConfig(StoredConfig config) {
		try {
			config.save();
		} catch (IOException e) {
			log.error("Error saving Git config", e);
		}
	}

}
