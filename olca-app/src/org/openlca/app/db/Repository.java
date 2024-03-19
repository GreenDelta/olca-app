package org.openlca.app.db;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.StoredConfig;
import org.openlca.app.collaboration.dialogs.AuthenticationDialog;
import org.openlca.app.collaboration.dialogs.AuthenticationDialog.GitCredentialsProvider;
import org.openlca.app.collaboration.util.WebRequests;
import org.openlca.app.rcp.Workspace;
import org.openlca.collaboration.api.CollaborationServer;
import org.openlca.core.database.IDatabase;
import org.openlca.git.repo.ClientRepository;
import org.openlca.git.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Repository extends ClientRepository {

	private static final Logger log = LoggerFactory.getLogger(Repository.class);
	public static Repository CURRENT;
	public static final String GIT_DIR = "repositories";
	public final String id;
	public final CollaborationServer server;

	private Repository(File gitDir, IDatabase database) throws IOException {
		super(gitDir, database);
		var url = url();
		if (url != null && (!url.startsWith("git@") && !url.startsWith("http")))
			throw new IllegalArgumentException("Unsupported protocol");
		var splitIndex = url.startsWith("git@")
				? url.lastIndexOf(":")
				: url.substring(0, url.lastIndexOf("/")).lastIndexOf("/");
		var serverUrl = url.substring(0, splitIndex);
		this.server = new CollaborationServer(serverUrl,
				() -> AuthenticationDialog.promptCredentials(serverUrl, user()));
		this.id = url.substring(splitIndex + 1);
	}

	private String url() {
		try (var git = new Git(this)) {
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

	public static Repository initialize(File gitDir, IDatabase database) {
		var repo = open(gitDir, database, true);
		if (repo != null) {
			repo.checkIfCollaborationServer();
		}
		return repo;
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

	public static File gitDir(String databaseName) {
		var repos = new File(Workspace.root(), GIT_DIR);
		return new File(repos, databaseName);
	}

	public static boolean isConnected() {
		return CURRENT != null;
	}

	public void checkIfCollaborationServer() {
		isCollaborationServer(server != null &&
				WebRequests.execute(
						() -> server.isCollaborationServer(), false));
	}

	public PersonIdent promptUser() {
		var ident = AuthenticationDialog.promptUser(server.url, user());
		if (ident == null)
			return null;
		user(ident.getName());
		return ident;
	}

	public GitCredentialsProvider promptCredentials() {
		var credentials = AuthenticationDialog.promptCredentials(server.url, user());
		if (credentials == null)
			return null;
		user(credentials.user);
		return credentials;
	}

	public GitCredentialsProvider promptToken() {
		var credentials = AuthenticationDialog.promptToken(server.url, user());
		if (credentials == null)
			return null;
		user(credentials.user);
		return credentials;
	}

	public String user() {
		return getConfig().getString("user", null, "name");
	}

	public void user(String user) {
		var config = getConfig();
		config.setString("user", null, "name", user);
		saveConfig(config);
	}

	public boolean isCollaborationServer() {
		return server != null && getConfig().getBoolean("remote", "origin", "isCollaborationServer", false);
	}

	public void isCollaborationServer(boolean value) {
		var config = getConfig();
		config.setBoolean("remote", "origin", "isCollaborationServer", value);
		saveConfig(config);
	}

	private void saveConfig(StoredConfig config) {
		try {
			config.save();
		} catch (IOException e) {
			log.error("Error saving Git config", e);
		}
	}

	@Override
	public void close() {
		if (server != null) {
			WebRequests.execute(server::close);
		}
		super.close();
		CURRENT = null;
	}
}
