package org.openlca.app.collaboration.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.openlca.app.rcp.RcpActivator;

public class CollaborationPreference extends AbstractPreferenceInitializer {

	public static final String FIRST_CONFIGURATION = "olca-collaboration-first-configuration";
	public static final String ONLY_FULL_COMMITS = "olca-collaboration-only-full-commits";
	public static final String CHECK_REFERENCES = "olca-collaboration-check-references";
	public static final String DISPLAY_COMMENTS = "olca-collaboration-display-comments";
	public static final String STORE_CONNECTION = "olca-collaboration-store-connection";

	@Override
	public void initializeDefaultPreferences() {
		var store = getStore();
		store.setDefault(FIRST_CONFIGURATION, true);
		store.setDefault(ONLY_FULL_COMMITS, true);
		store.setDefault(CHECK_REFERENCES, true);
		store.setDefault(DISPLAY_COMMENTS, false);
		store.setDefault(STORE_CONNECTION, true);
	}

	public static boolean firstConfiguration() {
		return is(FIRST_CONFIGURATION);
	}

	public static boolean onlyFullCommits() {
		return is(ONLY_FULL_COMMITS);
	}

	public static boolean checkReferences() {
		return is(CHECK_REFERENCES);
	}

	public static boolean commentsEnabled() {
		return is(DISPLAY_COMMENTS);
	}

	static void isConfigured() {
		getStore().setValue(FIRST_CONFIGURATION, false);
	}

	public static boolean storeConnection() {
		return is(STORE_CONNECTION);
	}

	public static void storeConnection(boolean value) {
		getStore().setValue(STORE_CONNECTION, value);
	}

	public static boolean didReadAnnouncement(String serverUrl, String announcementId) {
		var last = getStore().getString("olca-collaboration-last-announcement-" + serverUrl);
		if (last == null)
			return false;
		return last.equals(announcementId);
	}

	public static void markAnnouncementAsRead(String serverUrl, String announcementId) {
		getStore().setValue("olca-collaboration-last-announcement-" + serverUrl, announcementId);
	}

	private static boolean is(String key) {
		return getStore().getBoolean(key);
	}

	static IPreferenceStore getStore() {
		return RcpActivator.getDefault().getPreferenceStore();
	}

}
