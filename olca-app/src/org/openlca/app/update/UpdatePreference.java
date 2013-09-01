package org.openlca.app.update;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.openlca.app.RcpActivator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdatePreference extends AbstractPreferenceInitializer {

	private static final Logger log = LoggerFactory
			.getLogger(UpdatePreference.class);

	public static final String UPDATE_RYTHM_SECS = "olca-update-rythm-secs";
	public static final String UPDATE_RYTHM_NEVER = Long.toString(0);
	public static final String UPDATE_RYTHM_HOURLY = Long.toString(60 * 60);
	public static final String UPDATE_RYTHM_DAILY = Long.toString(60 * 60 * 24);
	public static final String UPDATE_RYTHM_WEEKLY = Long
			.toString(60 * 60 * 24 * 7);
	public static final String UPDATE_RYTHM_MONTHLY = Long
			.toString((long) (60 * 60 * 24 * 30.2));
	public static final String LAST_UPDATE_CHECK = "olca-update-lastcheck-ts";

	public static final String UPDATE_RYTHM_SECS_DEFAULT = UPDATE_RYTHM_DAILY;

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = getStore();
		store.setDefault(UPDATE_RYTHM_SECS, UPDATE_RYTHM_SECS_DEFAULT);
		store.setDefault(LAST_UPDATE_CHECK, 0);
	}

	public static boolean isUpdateEnabled() {
		return getUpdateRythmSecs() > 0;
	}

	public static long getUpdateRythmSecs() {
		IPreferenceStore store = getStore();
		String updateFreqStr = store.getString(UPDATE_RYTHM_SECS);
		long updateFreq;
		try {
			updateFreq = Long.parseLong(updateFreqStr);
		} catch (NumberFormatException nfe) {
			log.warn("Update rythm preference not a number: {}", updateFreqStr);
			updateFreq = -1;
		}
		if (updateFreq <= 0) {
			return -1;
		}
		return updateFreq;
	}

	public static long getLastUpdateCheckSecs() {
		IPreferenceStore store = getStore();
		long lastCheck = store.getLong(LAST_UPDATE_CHECK);
		if (lastCheck <= 0) {
			return 0;
		}
		return lastCheck;
	}

	static IPreferenceStore getStore() {
		return RcpActivator.getDefault().getPreferenceStore();
	}

	public static void setLastUpdateCheckSecs(long val) {
		getStore().setValue(LAST_UPDATE_CHECK, val);
	}

}
