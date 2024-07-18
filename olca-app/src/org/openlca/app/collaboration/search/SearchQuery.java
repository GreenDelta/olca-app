package org.openlca.app.collaboration.search;

import org.openlca.collaboration.client.CSClient;

public class SearchQuery {

	public CSClient client;
	public String query;
	public String type;
	public int page;
	public int pageSize = 10;

}
