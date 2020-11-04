package org.openlca.app.cloud.ui.preferences;

import org.openlca.app.cloud.TokenDialog;
import org.openlca.cloud.api.CredentialSupplier;
import org.openlca.cloud.api.RepositoryConfig;
import org.openlca.core.database.IDatabase;
import org.openlca.util.Strings;

public class CloudConfiguration {

	String url = "";
	String user = "";
	String password = "";
	boolean isDefault;

	CloudConfiguration() {

	}

	public String getUrl() {
		return url;
	}

	public String getUser() {
		return user;
	}

	public String getPassword() {
		return password;
	}

	public boolean isDefault() {
		return isDefault;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof CloudConfiguration))
			return false;
		CloudConfiguration other = (CloudConfiguration) obj;
		if (!Strings.nullOrEqual(url, other.url))
			return false;
		if (!Strings.nullOrEqual(user, other.user))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return user + "@" + url;
	}

	public RepositoryConfig toRepositoryConfig() {
		return toRepositoryConfig(null, null);
	}

	public RepositoryConfig toRepositoryConfig(IDatabase database) {
		return toRepositoryConfig(database, null);
	}

	public RepositoryConfig toRepositoryConfig(IDatabase database, String repositoryId) {
		RepositoryConfig config = new RepositoryConfig(database, url + "/ws", repositoryId);
		config.credentials = new CredentialSupplier(user, password);
		config.credentials.setTokenSupplier(TokenDialog::prompt);
		return config;
	}

}
