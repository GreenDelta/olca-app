package org.openlca.app.cloud.ui.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.openlca.app.rcp.RcpActivator;

public class CloudPreference extends AbstractPreferenceInitializer {

	public static final String CHECK_AGAINST_LIBRARIES = "olca-cloud-check-against-libraries";

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = getStore();
		store.setDefault(CHECK_AGAINST_LIBRARIES, true);
	}

	public static boolean doCheckAgainstLibraries() {
		IPreferenceStore store = getStore();
		return store.getBoolean(CHECK_AGAINST_LIBRARIES);
	}

	static IPreferenceStore getStore() {
		return RcpActivator.getDefault().getPreferenceStore();
	}

}
