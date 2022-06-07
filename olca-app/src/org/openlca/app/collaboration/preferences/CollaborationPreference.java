package org.openlca.app.collaboration.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.openlca.app.rcp.RcpActivator;

public class CollaborationPreference extends AbstractPreferenceInitializer {

	public static final String CHECK_RESTRICTIONS = "olca-collaboration-check-restrictions";
	public static final String CHECK_REFERENCES = "olca-collaboration-check-references";
	public static final String DISPLAY_COMMENTS = "olca-collaboration-display-comments";

	@Override
	public void initializeDefaultPreferences() {
		var store = getStore();
		store.setDefault(CHECK_RESTRICTIONS, false);
		store.setDefault(CHECK_REFERENCES, false);
		store.setDefault(DISPLAY_COMMENTS, false);
	}

	public static boolean checkRestrictions() {
		return is(CHECK_RESTRICTIONS);
	}

	public static boolean commentsEnabled() {
		return is(DISPLAY_COMMENTS);
	}

	public static boolean checkReferences() {
		return is(CHECK_REFERENCES);
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
