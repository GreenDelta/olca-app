package org.openlca.app.cloud.ui.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.openlca.app.rcp.RcpActivator;

public class CloudPreference extends AbstractPreferenceInitializer {

	public static final String CHECK_AGAINST_LIBRARIES = "olca-cloud-check-against-libraries";
	public static final String DEFAULT_HOST = "olca-cloud-default-host";
	public static final String DEFAULT_USER = "olca-cloud-default-user";
	public static final String DEFAULT_PASS = "olca-cloud-default-pass";

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = getStore();
		store.setDefault(CHECK_AGAINST_LIBRARIES, true);
	}

	public static boolean doCheckAgainstLibraries() {
		IPreferenceStore store = getStore();
		return store.getBoolean(CHECK_AGAINST_LIBRARIES);
	}

	public static String getDefaultHost() {
		IPreferenceStore store = getStore();
		return store.getString(DEFAULT_HOST);
	}

	public static String getDefaultUser() {
		IPreferenceStore store = getStore();
		return store.getString(DEFAULT_USER);
	}

	public static String getDefaultPass() {
		IPreferenceStore store = getStore();
		return store.getString(DEFAULT_PASS);
	}

	static IPreferenceStore getStore() {
		return RcpActivator.getDefault().getPreferenceStore();
	}

}
