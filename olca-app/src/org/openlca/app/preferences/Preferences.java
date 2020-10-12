package org.openlca.app.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.openlca.app.rcp.RcpActivator;
import org.openlca.app.util.Numbers;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Preferences extends AbstractPreferenceInitializer {

	public static final String NUMBER_ACCURACY = "NUMBER_ACCURACY";
	public static final String FORMAT_INPUT_VALUES = "FORMAT_INPUT_VALUES";
	public static final String LAST_IMPORT_FOLDER = "LAST_IMPORT_FOLDER";
	public static final String LAST_EXPORT_FOLDER = "LAST_EXPORT_FOLDER";

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = getStore();
		store.setDefault(NUMBER_ACCURACY, 5);
		store.setDefault(FORMAT_INPUT_VALUES, true);
	}

	public static void init() {
		Logger log = LoggerFactory.getLogger(Preferences.class);
		log.trace("init preferences");
		IPreferenceStore store = getStore();
		int acc = store.getDefaultInt(NUMBER_ACCURACY);
		Numbers.setDefaultAccuracy(acc);
		log.trace("preference {} = {}", NUMBER_ACCURACY, acc);
	}

	public static IPreferenceStore getStore() {
		return RcpActivator.getDefault().getPreferenceStore();
	}

	public static void set(String key, int value) {
		set(key, Integer.toString(value));
	}

	public static void set(String key, boolean value) {
		set(key, Boolean.toString(value));
	}

	public static void set(String key, String value) {
		if (key == null)
			return;
		var store = getStore();
		if (store == null)
			return;
		String val = value == null ? "" : value;
		store.setValue(key, val);
	}

	public static String get(String key) {
		if (key == null)
			return "";
		IPreferenceStore store = getStore();
		if (store == null)
			return "";
		return store.getString(key);
	}

	public static int getInt(String key) {
		String s = get(key);
		if (Strings.nullOrEmpty(s))
			return 0;
		try {
			return Integer.parseInt(s);
		} catch (Exception e) {
			return 0;
		}
	}

	public static boolean getBool(String key) {
		if (key == null)
			return false;
		var store = getStore();
		return store == null
				? false
				: store.getBoolean(key);
	}

}
