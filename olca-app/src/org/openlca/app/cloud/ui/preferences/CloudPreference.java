package org.openlca.app.cloud.ui.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.openlca.app.rcp.RcpActivator;

public class CloudPreference extends AbstractPreferenceInitializer {

	public static final String ENABLE = "olca-cloud-enable";
	public static final String CHECK_AGAINST_LIBRARIES = "olca-cloud-check-against-libraries";
	public static final String CHECK_REFERENCES = "olca-cloud-check-references";
	public static final String DISPLAY_COMMENTS = "olca-cloud-display-comments";

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = getStore();
		store.setDefault(ENABLE, false);
		store.setDefault(CHECK_AGAINST_LIBRARIES, true);
		store.setDefault(CHECK_REFERENCES, true);
		store.setDefault(DISPLAY_COMMENTS, false);
	}

	public static boolean doEnable() {
		return is(ENABLE);
	}

	public static boolean doCheckAgainstLibraries() {
		return is(CHECK_AGAINST_LIBRARIES);
	}

	public static boolean doDisplayComments() {
		return is(DISPLAY_COMMENTS);
	}

	public static boolean doCheckReferences() {
		return is(CHECK_REFERENCES);
	}
	
	public static boolean didReadAnnouncement(String serverUrl, String announcementId) {
		String last = getStore().getString("last-announcement-" + serverUrl);
		if (last == null)
			return false;
		return last.equals(announcementId);
	}

	public static void markAnnouncementAsRead(String serverUrl, String announcementId) {
		getStore().setValue("last-announcement-" + serverUrl, announcementId);
	}
	
	private static boolean is(String key) {
		IPreferenceStore store = getStore();
		return store.getBoolean(key);		
	}

	static IPreferenceStore getStore() {
		return RcpActivator.getDefault().getPreferenceStore();
	}

}
