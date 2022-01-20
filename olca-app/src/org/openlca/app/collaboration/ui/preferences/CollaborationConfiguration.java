package org.openlca.app.collaboration.ui.preferences;

import org.openlca.util.Strings;

public class CollaborationConfiguration {

	String url = "";
	String user = "";
	String password = "";
	boolean isDefault;

	CollaborationConfiguration() {
		
	}
	
	public String url() {
		return url;
	}

	public String user() {
		return user;
	}

	public String password() {
		return password;
	}

	public boolean isDefault() {
		return isDefault;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof CollaborationConfiguration))
			return false;
		var other = (CollaborationConfiguration) obj;
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
