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
import org.openlca.collaboration.client.CSClient;
import org.openlca.core.database.IDatabase;
import org.openlca.git.repo.ClientRepository;
import org.openlca.git.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Repository extends ClientRepository {

	private static final Logger log = LoggerFactory.getLogger(Repository.class);
	public static Repository CURRENT;
	public static final String GIT_DIR = "repositories";
	public final String url;
	public final String id;
	public final CSClient client;

	private Repository(File gitDir, IDatabase database) throws IOException {
		super(gitDir, database);
		this.url = url();
		if (url != null && (!url.startsWith("git@") && !url.startsWith("http")))
			throw new IllegalArgumentException("Unsupported protocol");
		var splitIndex = url.startsWith("git@")
				? url.lastIndexOf(":")
				: url.substring(0, url.lastIndexOf("/")).lastIndexOf("/");
		var serverUrl = url.substring(0, splitIndex);
		var isCollaborationServer = WebRequests.execute(
				() -> CSClient.isCollaborationServer(url), false);
		this.client = isCollaborationServer
				? new CSClient(serverUrl, () -> AuthenticationDialog.promptCredentials(serverUrl, user()))
				: null;
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
		return open(gitDir, database, true);
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

	public PersonIdent promptUser() {
		var ident = AuthenticationDialog.promptUser(url, user());
		if (ident == null)
			return null;
		user(ident.getName());
		return ident;
	}

	public GitCredentialsProvider promptCredentials() {
		var credentials = AuthenticationDialog.promptCredentials(url, user());
		if (credentials == null)
			return null;
		user(credentials.user);
		return credentials;
	}

	public GitCredentialsProvider promptToken() {
		var credentials = AuthenticationDialog.promptToken(url, user());
		if (credentials == null)
			return null;
		user(credentials.user);
		return credentials;
	}

	public boolean isCollaborationServer() {
		return client != null;
	}

	public String user() {
		return getConfig().getString("user", null, "name");
	}

	public void user(String user) {
		var config = getConfig();
		config.setString("user", null, "name", user);
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
		if (client != null) {
			WebRequests.execute(client::close);
		}
		super.close();
		CURRENT = null;
	}
}
