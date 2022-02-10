package org.openlca.app.db;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.PersonIdent;
import org.openlca.app.collaboration.api.InMemoryCredentialSupplier;
import org.openlca.app.collaboration.api.RepositoryClient;
import org.openlca.app.collaboration.api.RepositoryConfig;
import org.openlca.core.database.IDatabase;
import org.openlca.git.Config;
import org.openlca.git.ObjectIdStore;
import org.openlca.git.find.Commits;
import org.openlca.git.find.Datasets;
import org.openlca.git.find.Diffs;
import org.openlca.git.find.Entries;
import org.openlca.git.find.Ids;
import org.openlca.git.find.References;
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
	public final Diffs diffs;
	public final Entries entries;

	private Repository(IDatabase database, File gitDir) throws IOException {
		git = new FileRepository(gitDir);
		config = RepositoryConfig.of(git, new InMemoryCredentialSupplier());
		client = RepositoryClient.isCollaborationServer(config)
				? new RepositoryClient(config)
				: null;
		var storeFile = new File(RepositoryConfig.getGirDir(database), "object-id.store");
		workspaceIds = ObjectIdStore.open(storeFile);
		commits = Commits.of(git);
		datasets = Datasets.of(git);
		references = References.of(git);
		diffs = Diffs.of(git);
		entries = Entries.of(git);
		ids = Ids.of(git);
	}

	public static Repository get() {
		return repository;
	}

	public static void connect(IDatabase database) {
		if (isConnected()) {
			disconnect();
		}
		try {
			var gitDir = RepositoryConfig.getGirDir(database);
			if (!gitDir.exists() || !gitDir.isDirectory() || gitDir.listFiles().length == 0)
				return;
			repository = new Repository(database, gitDir);
		} catch (IOException e) {
			log.error("Error opening git repo", e);
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

	public Config toConfig() {
		// TODO correct person ident
		var committer = new PersonIdent(config.credentials.username(), config.credentials.username() + "@email.com");
		return new Config(Database.get(), workspaceIds, git, committer);
	}

	public boolean isCollaborationServer() {
		return client != null;
	}

}
