package org.openlca.app.collaboration.search;

import org.openlca.app.collaboration.model.SearchResult;
import org.openlca.app.collaboration.util.RepositoryClients;

public class Search {

	public static SearchResult run(SearchQuery query) {
		if (query.client == null) {
			var clients = RepositoryClients.get();
			if (clients.isEmpty())
				return new SearchResult();
			query.client = clients.get(0);
		}
		return query.client.search(query.query, query.type, query.page, query.pageSize);
	}

}
