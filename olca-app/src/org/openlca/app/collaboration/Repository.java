package org.openlca.app.collaboration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.openlca.app.M;
import org.openlca.app.collaboration.dialogs.AuthenticationDialog;
import org.openlca.app.collaboration.dialogs.AuthenticationDialog.GitCredentialsProvider;
import org.openlca.app.collaboration.util.CredentialStore;
import org.openlca.app.collaboration.util.WebRequests;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.util.MsgBox;
import org.openlca.collaboration.client.CSClient;
import org.openlca.core.database.IDatabase;
import org.openlca.git.repo.ClientRepository;
import org.openlca.git.util.Constants;
import org.openlca.jsonld.LibraryLink;
import org.openlca.util.Dirs;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Repository extends ClientRepository {

	private static final Logger log = LoggerFactory.getLogger(Repository.class);
	public static Repository CURRENT;
	public static final String GIT_DIR = "repositories";
	public final String url;
	public final String serverUrl;
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
		this.serverUrl = url.substring(0, splitIndex);
		var isCollaborationServer = WebRequests.execute(
				() -> CSClient.isCollaborationServer(serverUrl), false);
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

	public static void delete(String databaseName) {
		var gitDir = Repository.gitDir(Database.get().getName());
		try {
			Dirs.delete(gitDir);
		} finally {
			if (gitDir.exists() && !Dirs.isEmpty(gitDir)) {
				MsgBox.warning(M.RepositoryNotDeleted + "\r\n\r\n" + gitDir.getAbsolutePath());
			}
		}
	}

	public static boolean isConnected() {
		return CURRENT != null;
	}

	public PersonIdent promptUser() {
		var ident = AuthenticationDialog.promptUser(serverUrl, user());
		if (ident == null)
			return null;
		user(ident.getName());
		return ident;
	}

	public GitCredentialsProvider promptCredentials() {
		var credentials = AuthenticationDialog.promptCredentials(serverUrl, user());
		if (credentials == null)
			return null;
		user(credentials.user);
		return credentials;
	}

	public GitCredentialsProvider promptToken() {
		var credentials = AuthenticationDialog.promptToken(serverUrl, user());
		if (credentials == null)
			return null;
		user(credentials.user);
		return credentials;
	}

	public boolean librariesChanged() {
		var info = getInfo();
		var libsBefore = info == null ? new ArrayList<LibraryLink>() : info.libraries();
		var libsNow = LibraryLink.of(Database.get().getLibraries());
		if (libsBefore.size() != libsNow.size())
			return true;
		for (var lib : libsBefore)
			if (!libsNow.contains(lib))
				return true;
		for (var lib : libsNow)
			if (!libsBefore.contains(lib))
				return true;
		return false;
	}

	public boolean isCollaborationServer() {
		return client != null;
	}

	public String user() {
		// from repo config
		var repoConfigFile = new File(dir, "config");
		if (repoConfigFile.exists()) {
			var repoConfig = new FileBasedConfig(repoConfigFile, getFS());
			if (repoConfig != null) {
				var name = repoConfig.getString("user", null, "name");
				if (Strings.isNotBlank(name))
					return name;
			}
		}
		// from server config
		var username = CredentialStore.getUsername(serverUrl);
		if (Strings.isNotBlank(username))
			return username;
		// from global config
		return getConfig().getString("user", null, "email");
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
