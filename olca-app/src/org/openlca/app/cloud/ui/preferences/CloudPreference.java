package org.openlca.app.cloud.ui.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.openlca.app.rcp.RcpActivator;

public class CloudPreference extends AbstractPreferenceInitializer {

	public static final String CHECK_AGAINST_LIBRARIES = "olca-cloud-check-against-libraries";
	public static final String DISPLAY_COMMENTS = "olca-cloud-display-comments";

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = getStore();
		store.setDefault(CHECK_AGAINST_LIBRARIES, true);
	}

	public static boolean doCheckAgainstLibraries() {
		IPreferenceStore store = getStore();
		return store.getBoolean(CHECK_AGAINST_LIBRARIES);
	}

	public static boolean doDisplayComments() {
		IPreferenceStore store = getStore();
		return store.getBoolean(DISPLAY_COMMENTS);
	}

	static IPreferenceStore getStore() {
		return RcpActivator.getDefault().getPreferenceStore();
	}

}
