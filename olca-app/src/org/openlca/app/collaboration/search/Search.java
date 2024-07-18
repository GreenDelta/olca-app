package org.openlca.app.collaboration.search;

import org.openlca.app.collaboration.navigation.ServerConfigurations;
import org.openlca.app.collaboration.util.WebRequests;
import org.openlca.collaboration.model.Dataset;
import org.openlca.collaboration.model.SearchResult;

public class Search {

	public static SearchResult<Dataset> run(SearchQuery query) {
		if (query.client == null) {
			var clients = ServerConfigurations.get();
			if (clients.isEmpty())
				return new SearchResult<Dataset>();
			query.client = clients.get(0).createClient();
		}
		return WebRequests.execute(
				() -> query.client.search(query.query, query.type, query.page, query.pageSize),
				new SearchResult<Dataset>());
	}

}
