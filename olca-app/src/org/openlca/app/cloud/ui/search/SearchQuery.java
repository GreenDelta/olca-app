package org.openlca.app.cloud.ui.search;

import org.openlca.app.cloud.ui.preferences.CloudConfiguration;
import org.openlca.app.cloud.ui.preferences.CloudConfigurations;
import org.openlca.app.db.Database;
import org.openlca.cloud.api.RepositoryClient;
import org.openlca.cloud.api.RepositoryConfig;
import org.openlca.core.model.ModelType;

public class SearchQuery {
	
	private CloudConfiguration configuration;
	private RepositoryClient client;
	public String query;
	public ModelType type;
	public int page;
	public int pageSize;
	
	CloudConfiguration getConfiguration() {
		return configuration;
	}

	void setConfiguration(CloudConfiguration configuration) {
		this.configuration = configuration;
		RepositoryConfig config = configuration.toRepositoryConfig(Database.get());
		client = new RepositoryClient(config);
	}

	RepositoryClient getClient() {
		if (client == null) {
			setConfiguration(CloudConfigurations.getDefault());
		}
		return client;
	}

}
