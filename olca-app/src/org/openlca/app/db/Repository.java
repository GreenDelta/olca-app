package org.openlca.app.db;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.StoredConfig;
import org.openlca.app.collaboration.dialogs.AuthenticationDialog;
import org.openlca.app.collaboration.util.WebRequests;
import org.openlca.app.rcp.Workspace;
import org.openlca.collaboration.api.CollaborationServer;
import org.openlca.core.database.IDatabase;
import org.openlca.git.repo.ClientRepository;
import org.openlca.git.util.Constants;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Repository extends ClientRepository {

	private static final Logger log = LoggerFactory.getLogger(Repository.class);
	public static Repository CURRENT;
	public static final String GIT_DIR = "repositories";
	public final CollaborationServer server;
	private String password;

	private Repository(File gitDir, IDatabase database) throws IOException {
		super(gitDir, database);
		server = server(this);
	}

	public static void checkIfCollaborationServer() {
		checkIfCollaborationServer(CURRENT);
	}

	public static void checkIfCollaborationServer(FileRepository gitRepo) throws IOException {
		if (gitRepo == null)
			return;
		var server = server(gitRepo);
		isCollaborationServer(gitRepo.getConfig(),
				server != null && WebRequests.execute(server::isCollaborationServer, false));
	}

	public static void checkIfCollaborationServer(Repository repo) {
		if (repo == null)
			return;
		repo.isCollaborationServer(
				repo.server != null && WebRequests.execute(repo.server::isCollaborationServer, false));
	}

	public static Repository open(File gitDir, IDatabase database) {
		return open(gitDir, database, false);
	}

	private static Repository open(File gitDir, IDatabase database, boolean createIfNotExists) {
		if (CURRENT != null) {
			CURRENT.close();
			CURRENT = null;
		}
		try {
			if ((!gitDir.exists() && !gitDir.isDirectory() || gitDir.listFiles().length == 0) && !createIfNotExists)
				return null;
			CURRENT = new Repository(gitDir, database);
			if (!gitDir.exists())
				CURRENT.create(true);
			return CURRENT;
		} catch (IOException e) {
			log.error("Error opening Git repo", e);
			return null;
		}
	}

	public static Repository initialize(File gitDir, IDatabase database) {
		var repo = open(gitDir, database, true);
		checkIfCollaborationServer(repo);
		return repo;
	}

	public static File gitDir(String databaseName) {
		var repos = new File(Workspace.root(), GIT_DIR);
		return new File(repos, databaseName);
	}

	public static CollaborationServer server(org.eclipse.jgit.lib.Repository repo) throws IOException {
		var url = url(repo);
		if (Strings.nullOrEmpty(url))
			return null;
		if (url.startsWith("git@")) {
			var splitIndex = url.lastIndexOf(":");
			var serverUrl = url.substring(0, splitIndex);
			return new CollaborationServer(serverUrl, () -> AuthenticationDialog.promptCredentials(serverUrl));
		} else if (url.startsWith("http")) {
			var splitIndex = url.substring(0, url.lastIndexOf("/")).lastIndexOf("/");
			var serverUrl = url.substring(0, splitIndex);
			return new CollaborationServer(serverUrl, () -> AuthenticationDialog.promptCredentials(serverUrl));
		}
		throw new IllegalArgumentException("Unsupported protocol");
	}

	public String getId() {
		var url = url(this);
		if (Strings.nullOrEmpty(url))
			return null;
		if (url.startsWith("git@")) {
			var splitIndex = url.lastIndexOf(":");
			return url.substring(splitIndex + 1);
		} else if (url.startsWith("http")) {
			var splitIndex = url.substring(0, url.lastIndexOf("/")).lastIndexOf("/");
			return url.substring(splitIndex + 1);
		}
		throw new IllegalArgumentException("Unsupported protocol");
	}

	public static boolean isConnected() {
		return CURRENT != null;
	}

	@Override
	public void close() {
		if (server != null) {
			WebRequests.execute(server::close);
		}
		super.close();
		CURRENT = null;
	}

	public String url() {
		return url(this);
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

	public String user() {
		return getConfig().getString("user", null, "name");
	}

	public void user(String user) {
		if (!user.equals(user())) {
			useTwoFactorAuth(false);
		}
		var config = getConfig();
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
		return getConfig().getBoolean("user", null, "useTwoFactorAuth", false);
	}

	public void useTwoFactorAuth(boolean value) {
		var config = getConfig();
		config.setString("user", null, "useTwoFactorAuth", Boolean.toString(value));
		saveConfig(config);
	}

	public static boolean isCollaborationServer(StoredConfig config) {
		return config.getBoolean("remote", "origin", "isCollaborationServer", false);
	}

	public boolean isCollaborationServer() {
		return isCollaborationServer(getConfig()) && server != null;
	}

	public static void isCollaborationServer(StoredConfig config, boolean value) {
		config.setBoolean("remote", "origin", "isCollaborationServer", value);
		saveConfig(config);
	}

	public void isCollaborationServer(boolean value) {
		isCollaborationServer(getConfig(), value);
	}

	private static void saveConfig(StoredConfig config) {
		try {
			config.save();
		} catch (IOException e) {
			log.error("Error saving Git config", e);
		}
	}

}
