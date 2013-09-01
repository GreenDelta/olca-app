package org.openlca.ilcd.network.rcp.ui;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.openlca.app.RcpActivator;
import org.openlca.ilcd.io.NetworkClient;

public class Preference extends AbstractPreferenceInitializer {

	public static final String URL = "ilcd-network-url";
	public static final String USER = "ilcd-network-user";
	public static final String PASSWORD = "ilcd-network-password";

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = RcpActivator.getDefault().getPreferenceStore();
		store.setDefault(PASSWORD, "");
		store.setDefault(USER, "user");
		store.setDefault(URL, "http://host-adress.web/path/resource");
	}

	public static String getPassword() {
		return valueOf(PASSWORD);
	}

	public static String getUser() {
		return valueOf(USER);
	}

	public static String getUrl() {
		return valueOf(URL);
	}

	public static NetworkClient createClient() {
		NetworkClient client = new NetworkClient(getUrl(), getUser(),
				getPassword());
		return client;
	}

	private static String valueOf(String name) {
		IPreferenceStore store = RcpActivator.getDefault().getPreferenceStore();
		return store.getString(name);
	}

}
