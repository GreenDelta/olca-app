package org.openlca.app.collaboration.search;

import org.openlca.app.collaboration.api.RepositoryClient;
import org.openlca.core.model.ModelType;

public class SearchQuery {

	public RepositoryClient client;
	public String query;
	public ModelType type;
	public int page;
	public int pageSize;

}
