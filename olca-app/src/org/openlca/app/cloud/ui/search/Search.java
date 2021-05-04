package org.openlca.app.cloud.ui.search;

import org.openlca.app.cloud.WebRequestExceptions;
import org.openlca.cloud.model.data.DatasetEntry;
import org.openlca.cloud.util.WebRequests.WebRequestException;

import com.greendelta.search.wrapper.SearchResult;

public class Search implements Runnable {

	private SearchQuery query;
	public SearchResult<DatasetEntry> result;

	public Search(SearchQuery query) {
		this.query = query;
	}

	public void run() {
		try {
			result = query.getClient().search(query.query, query.page, query.pageSize, query.type);
		} catch (WebRequestException e) {
			result = new SearchResult<>();
			WebRequestExceptions.handle(e);
		}
	}

}