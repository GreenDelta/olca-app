package org.openlca.app.collaboration.api;

import java.io.File;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.openlca.app.rcp.Workspace;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.config.DatabaseConfig;
import org.openlca.git.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryConfig {

	private static final Logger log = LoggerFactory.getLogger(RepositoryConfig.class);
	public static final String GIT_DIR = "repositories";
	public final String serverUrl;
	public final String repositoryId;
	public final String apiUrl;

	private RepositoryConfig(String serverUrl, String repositoryId) {
		this.serverUrl = serverUrl;
		this.repositoryId = repositoryId;
		this.apiUrl = serverUrl + "/ws";
	}

	@SuppressWarnings("resource")
	public static RepositoryConfig of(FileRepository repo) {
		try {
			var configs = new Git(repo).remoteList().call();
			var config = configs.stream()
					.filter(c -> c.getName().equals(Constants.DEFAULT_REMOTE))
					.findFirst()
					.orElse(null);
			if (config == null || config.getURIs().isEmpty())
				throw new IllegalStateException("No remote URI configured");
			var uri = config.getURIs().get(0);
			var url = uri.toString();
			if (url.startsWith("git@")) {
				var splitIndex = url.lastIndexOf(":");
				var serverUrl = url.substring(0, splitIndex);
				var repositoryId = url.substring(splitIndex + 1);
				return new RepositoryConfig(serverUrl, repositoryId);
			}
			if (url.startsWith("http")) {
				var splitIndex = url.substring(0, url.lastIndexOf("/")).lastIndexOf("/");
				var serverUrl = url.substring(0, splitIndex);
				var repositoryId = url.substring(splitIndex + 1);
				return new RepositoryConfig(serverUrl, repositoryId);
			}
			return null;
		} catch (Exception e) {
			log.error("Error loading Git config", e);
			return null;
		}
	}

	public static File getGitDir(DatabaseConfig config) {
		var repos = new File(Workspace.root(), GIT_DIR);
		return new File(repos, config.name());
	}

	public static File getGitDir(IDatabase database) {
		var repos = new File(Workspace.root(), GIT_DIR);
		return new File(repos, database.getName());
	}

	public String url() {
		if (serverUrl.startsWith("git@"))
			return serverUrl + ":" + repositoryId;
		return serverUrl + "/" + repositoryId;
	}

}
