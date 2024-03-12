package org.openlca.app.collaboration.util;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.collaboration.api.CollaborationServer;
import org.openlca.core.database.config.DerbyConfig;

public class CollaborationServers {
	
	public static List<CollaborationServer> get() {
		return Database.getConfigurations().getDerbyConfigs().stream()
				.map(CollaborationServers::get)
				.filter(Objects::nonNull)
				.toList();
	}

	private static CollaborationServer get(DerbyConfig dbConfig) {
		var gitDir = Repository.gitDir(dbConfig.name());
		if (!gitDir.exists())
			return null;
		try (var repo = new FileRepository(gitDir)) {
			Repository.checkIfCollaborationServer(repo);
			var config = repo.getConfig();
			if (!Repository.isCollaborationServer(config))
				return null;
			return Repository.server(repo);
		} catch (IOException e) {
			return null;
		}
	}

}
