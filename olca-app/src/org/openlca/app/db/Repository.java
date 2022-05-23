package org.openlca.app.db;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.PersonIdent;
import org.openlca.app.collaboration.api.BasicCredentials;
import org.openlca.app.collaboration.api.RepositoryClient;
import org.openlca.app.collaboration.api.RepositoryConfig;
import org.openlca.app.util.Input;
import org.openlca.core.database.IDatabase;
import org.openlca.git.GitConfig;
import org.openlca.git.ObjectIdStore;
import org.openlca.git.find.Commits;
import org.openlca.git.find.Datasets;
import org.openlca.git.find.Entries;
import org.openlca.git.find.Ids;
import org.openlca.git.find.References;
import org.openlca.git.util.History;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Repository {

	private static final Logger log = LoggerFactory.getLogger(Repository.class);
	private static Repository repository;
	public final FileRepository git;
	public final RepositoryConfig config;
	public final RepositoryClient client;
	public final ObjectIdStore workspaceIds;
	public final Commits commits;
	public final Datasets datasets;
	public final Ids ids;
	public final References references;
	public final Entries entries;
	public final History history;
	public PersonIdent user;

	private Repository(IDatabase database, File gitDir) throws IOException {
		git = new FileRepository(gitDir);
		config = RepositoryConfig.of(git);
		client = RepositoryClient.isCollaborationServer(config)
				? new RepositoryClient(config)
				: null;
		var storeFile = new File(RepositoryConfig.getGitDir(database), "object-id.store");
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

	public static Repository connect(IDatabase database) {
		if (isConnected()) {
			disconnect();
		}
		try {
			var gitDir = RepositoryConfig.getGitDir(database);
			if (!gitDir.exists() || !gitDir.isDirectory() || gitDir.listFiles().length == 0)
				return null;
			repository = new Repository(database, gitDir);
			return repository;
		} catch (IOException e) {
			log.error("Error opening Git repo", e);
			return null;
		}
	}

	public static boolean isConnected() {
		return repository != null;
	}

	public static void disconnect() {
		if (repository == null)
			return;
		repository.git.close();
		repository = null;
	}

	public boolean isCollaborationServer() {
		return client != null;
	}

	public GitConfig toConfig() {
		return new GitConfig(Database.get(), Repository.get().workspaceIds, Repository.get().git);
	}

	public PersonIdent promptCommitter() {
		if (user != null)
			return user;
		var username = Input.promptString("Committer", "Please enter your username", "");
		if (Strings.nullOrEmpty(username))
			return null;
		// TODO
		user = new PersonIdent(username, username + "@email.com");
		return user;
	}

	public static BasicCredentials promptCredentials() {
		var credentials = BasicCredentials.prompt();
		if (credentials == null)
			return null;
		if (repository != null) {
			repository.user = new PersonIdent(credentials.username(), credentials.username() + "@email.com");
		}
		return credentials;
	}

}
