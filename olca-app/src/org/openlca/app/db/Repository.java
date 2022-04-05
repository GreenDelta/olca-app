package org.openlca.app.db;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.PersonIdent;
import org.openlca.app.collaboration.api.InMemoryCredentialSupplier;
import org.openlca.app.collaboration.api.RepositoryClient;
import org.openlca.app.collaboration.api.RepositoryConfig;
import org.openlca.core.database.IDatabase;
import org.openlca.git.GitConfig;
import org.openlca.git.ObjectIdStore;
import org.openlca.git.find.Commits;
import org.openlca.git.find.Datasets;
import org.openlca.git.find.Diffs;
import org.openlca.git.find.Entries;
import org.openlca.git.find.Ids;
import org.openlca.git.find.References;
import org.openlca.git.model.Commit;
import org.openlca.git.util.Constants;
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
	public final Diffs diffs;
	public final Entries entries;

	private Repository(IDatabase database, File gitDir) throws IOException {
		git = new FileRepository(gitDir);
		config = RepositoryConfig.of(git, new InMemoryCredentialSupplier());
		client = RepositoryClient.isCollaborationServer(config)
				? new RepositoryClient(config)
				: null;
		var storeFile = new File(RepositoryConfig.getGitDir(database), "object-id.store");
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

	public boolean isAheadOf(Commit commit) {
		var localCommitId = commits.resolve(Constants.LOCAL_BRANCH);
		return commits.find()
				.after(localCommitId)
				.until(commit.id)
				.all()
				.contains(commit);
	}

	public List<Commit> historyOf(String ref) {
		return commits.find().refs(ref).all();
	}

	public List<Commit> getAhead() {
		return getAhead(historyOf(Constants.LOCAL_REF), historyOf(Constants.REMOTE_REF));
	}

	public List<Commit> getBehind() {
		return getAhead(historyOf(Constants.REMOTE_REF), historyOf(Constants.LOCAL_REF));
	}
	
	private static List<Commit> getAhead(List<Commit> left, List<Commit> right) {
		var diff = new ArrayList<Commit>();
		for (var i = left.size() - 1; i >= 0; i--) {
			if (right.contains(left.get(i)))
				return diff;
			diff.add(left.get(i));
		}
		return diff;
	}

	public static void connect(IDatabase database) {
		if (isConnected()) {
			disconnect();
		}
		try {
			var gitDir = RepositoryConfig.getGitDir(database);
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

	public PersonIdent personIdent() {
		// TODO
		var username = config.credentials.username();
		if (Strings.nullOrEmpty(username))
			return null;
		return new PersonIdent(username, username + "@email.com");
	}

	public boolean isCollaborationServer() {
		return client != null;
	}

	public GitConfig toConfig() {
		var committer = personIdent();
		if (committer == null)
			return null;
		return new GitConfig(Database.get(), Repository.get().workspaceIds, Repository.get().git, committer);
	}

}
