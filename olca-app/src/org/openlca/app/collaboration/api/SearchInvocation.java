package org.openlca.app.collaboration.api;

import org.openlca.core.model.ModelType;

/**
 * Invokes a web service call to search for data sets
 */
class SearchInvocation {

	private static final String PATH = "/public/search";
	String baseUrl;
	String sessionId;
	String query;
	int page = 1;
	int pageSize = 10;
	ModelType type;
	String repositoryId;


	/**
	 * Retrieves a search result
	 *
	 * @return The search result
	 */
	// TODO migrate to new Search API
//	SearchResult<Object> execute() throws WebRequestException {
//		Valid.checkNotEmpty(baseUrl, "base url");
//		var url = baseUrl + PATH + "?page=" + page + "&pageSize=" + pageSize;
//		if (!Strings.isNullOrEmpty(query)) {
//			url += "&query=" + query;
//		}
//		if (type != null) {
//			url += "&type=" + type.name();
//		}
//		if (!Strings.isNullOrEmpty(repositoryId)) {
//			url += "&repositoryId=" + repositoryId;
//		}
//		var response = WebRequests.call(Type.GET, url, sessionId);
//		return new Gson().fromJson(response.getEntity(String.class),
//				new TypeToken<SearchResult<Object>>() {
//				}.getType());
//	}

}
