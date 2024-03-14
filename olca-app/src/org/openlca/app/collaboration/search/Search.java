package org.openlca.app.collaboration.search;

import org.openlca.app.collaboration.navigation.ServerConfigurations;
import org.openlca.app.collaboration.util.WebRequests;
import org.openlca.collaboration.model.Dataset;
import org.openlca.collaboration.model.SearchResult;

public class Search {

	public static SearchResult<Dataset> run(SearchQuery query) {
		if (query.server == null) {
			var clients = ServerConfigurations.get();
			if (clients.isEmpty())
				return new SearchResult<Dataset>();
			query.server = clients.get(0);
		}
		return WebRequests.execute(
				() -> query.server.search(query.query, query.type, query.page, query.pageSize),
				new SearchResult<Dataset>());
	}

}
