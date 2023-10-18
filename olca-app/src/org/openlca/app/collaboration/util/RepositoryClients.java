package org.openlca.app.collaboration.util;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.openlca.app.collaboration.api.RepositoryClient;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.core.database.config.DerbyConfig;

public class RepositoryClients {
	
	public static List<RepositoryClient> get() {
		return Database.getConfigurations().getDerbyConfigs().stream()
				.map(RepositoryClients::getClient)
				.filter(Objects::nonNull)
				.toList();
	}

	private static RepositoryClient getClient(DerbyConfig dbConfig) {
		var gitDir = Repository.gitDir(dbConfig.name());
		if (!gitDir.exists())
			return null;
		try (var repo = new FileRepository(gitDir)) {
			Repository.checkIfCollaborationServer(repo);
			var config = repo.getConfig();
			if (!Repository.isCollaborationServer(config))
				return null;
			return Repository.client(repo);
		} catch (IOException e) {
			return null;
		}
	}

}
