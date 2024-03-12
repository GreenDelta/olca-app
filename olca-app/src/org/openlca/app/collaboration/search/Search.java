package org.openlca.app.collaboration.search;

import org.openlca.app.collaboration.util.CollaborationServers;
import org.openlca.app.collaboration.util.WebRequests;
import org.openlca.collaboration.api.SearchInvocation.SearchResult;

public class Search {

	public static SearchResult run(SearchQuery query) {
		if (query.server == null) {
			var clients = CollaborationServers.get();
			if (clients.isEmpty())
				return new SearchResult();
			query.server = clients.get(0);
		}
		return WebRequests.execute(
				() -> query.server.search(query.query, query.type, query.page, query.pageSize),
				new SearchResult());
	}

}
