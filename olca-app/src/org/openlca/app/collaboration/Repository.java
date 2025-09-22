package org.openlca.app.collaboration;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.WindowCacheConfig;
import org.openlca.app.collaboration.dialogs.AuthenticationDialog;
import org.openlca.app.collaboration.dialogs.AuthenticationDialog.GitCredentialsProvider;
import org.openlca.app.collaboration.util.WebRequests;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.Workspace;
import org.openlca.collaboration.client.CSClient;
import org.openlca.core.database.DataPackage;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.descriptors.Descriptors;
import org.openlca.git.actions.GitInit;
import org.openlca.git.repo.ClientRepository;
import org.openlca.git.util.Constants;
import org.openlca.util.Dirs;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Repository extends ClientRepository {

	private static final Logger log = LoggerFactory.getLogger(Repository.class);
	private static Map<DataPackage, Repository> OPEN = new HashMap<>();
	private static Descriptors DESCRIPTORS;
	private static final String GIT_DIR = "repositories";
	public final String url;
	public final String serverUrl;
	public final String id;
	public final DataPackage dataPackage;
	public final CSClient client;

	private Repository(IDatabase database, DataPackage dataPackage) throws IOException {
		super(gitDir(database, dataPackage), database, dataPackage, descriptors());
		this.url = url();
		if (url != null && (!url.startsWith("git@") && !url.startsWith("http")))
			throw new IllegalArgumentException("Unsupported protocol");
		this.dataPackage = dataPackage;
		if (url != null) {
			var splitIndex = url.startsWith("git@")
					? url.lastIndexOf(":")
					: url.substring(0, url.lastIndexOf("/")).lastIndexOf("/");
			this.serverUrl = url.substring(0, splitIndex);
			this.id = url.substring(splitIndex + 1);
			var isCollaborationServer = WebRequests.execute(
					() -> CSClient.isCollaborationServer(serverUrl), false);
			this.client = isCollaborationServer
					? new CSClient(serverUrl, () -> AuthenticationDialog.promptCredentials(serverUrl, user()))
					: null;
		} else {
			this.serverUrl = null;
			this.id = null;
			this.client = null;
		}
	}

	public static Descriptors descriptors() {
		if (DESCRIPTORS == null) {
			DESCRIPTORS = Descriptors.of(Database.get());
		}
		return DESCRIPTORS;
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

	public static Repository get() {
		return OPEN.get(null);
	}

	public static Repository get(DataPackage dataPackage) {
		return OPEN.get(dataPackage);
	}

	public static Repository initialize(IDatabase database, String url) {
		return initialize(database, null, url);
	}

	public static Repository initialize(IDatabase database, DataPackage dataPackage, String url) {
		var gitDir = gitDir(database, dataPackage);
		try {
			GitInit.in(gitDir).remoteUrl(url).run();
		} catch (GitAPIException | URISyntaxException e) {
			log.warn("Error initializing git repo from " + url, e);
			Dirs.delete(gitDir);
			return null;
		}
		try {
			var repo = new Repository(database, dataPackage);
			if (!repo.exists()) {
				repo.close();
				return null;
			}
			OPEN.put(dataPackage, repo);
			return repo;
		} catch (IOException e) {
			log.error("Error opening Git repo", e);
			return null;
		}
	}

	public static void rename(String oldName, String newName) {
		var oldGitDir = Repository.gitDir(oldName, null);
		if (oldGitDir.exists()) {
			var newGitFolder = Repository.gitDir(newName, null);
			if (!oldGitDir.renameTo(newGitFolder)) {
				log.error("failed to rename repository dir");
			}
		}
	}

	public static void delete(String databaseName) {
		delete(databaseName, null);
	}

	public static void delete(String databaseName, DataPackage dataPackage) {
		var repo = OPEN.get(dataPackage);
		if (repo != null) {
			repo.close();
		}
		var gitDir = gitDir(databaseName, dataPackage != null ? dataPackage.name() : null);
		Dirs.delete(gitDir);
	}

	public static void open(IDatabase database) {
		closeAll();
		open(database, null);
		for (var dataPackage : database.getDataPackages().getAll()) {
			open(database, dataPackage);
		}
	}

	private static Repository open(IDatabase database, DataPackage dataPackage) {
		DESCRIPTORS = null;
		var repo = OPEN.get(dataPackage);
		if (repo != null) {
			repo.close();
		}
		try {
			repo = new Repository(database, dataPackage);
			if (!repo.exists()) {
				if (dataPackage != null && dataPackage.isRepository())
					return initialize(database, dataPackage, dataPackage.url());
				repo.close();
				return null;
			}
			OPEN.put(dataPackage, repo);
			return repo;
		} catch (IOException e) {
			log.error("Error opening Git repo", e);
			return null;
		}
	}

	private static File gitDir(IDatabase database, DataPackage dataPackage) {
		return gitDir(database.getName(), dataPackage != null ? dataPackage.name() : null);
	}

	public static File gitDir(String databaseName) {
		return gitDir(databaseName, null);
	}

	private static File gitDir(String databaseName, String packageName) {
		var repos = new File(Workspace.root(), GIT_DIR);
		var root = new File(repos, databaseName);
		if (Strings.nullOrEmpty(packageName))
			return root;
		return new File(root, "x-" + packageName);
	}

	public static boolean isConnected() {
		var db = Database.get();
		if (db == null)
			return false;
		return Repository.get() != null;
	}

	public static void closeAll() {
		DESCRIPTORS = null;
		for (var open : new ArrayList<>(OPEN.values())) {
			open.close();
		}
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

	private boolean exists() {
		if (dir == null || !dir.exists() || !dir.isDirectory())
			return false;
		return new File(dir, "config").exists() && new File(dir, "HEAD").exists();
	}

	public void disconnect() {
		close();
		// TODO this is a workaround to avoid open file handles that jgit
		// is holding (see https://github.com/eclipse-jgit/jgit/issues/155)
		new WindowCacheConfig().install();
		if (dir.listFiles() == null)
			return;
		// keep sub repository directories
		for (var child : dir.listFiles()) {
			if (child.isDirectory() && child.getName().startsWith("x-"))
				continue;
			Dirs.delete(child);
		}
		if (dir.listFiles() != null && dir.listFiles().length == 0) {
			Dirs.delete(dir);
		}
	}

	@Override
	public void close() {
		if (client != null) {
			WebRequests.execute(client::close);
		}
		OPEN.remove(dataPackage);
		super.close();
	}
}
