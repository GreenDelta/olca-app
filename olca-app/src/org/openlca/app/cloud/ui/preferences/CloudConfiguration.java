package org.openlca.app.cloud.ui.preferences;

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

}
