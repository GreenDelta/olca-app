package org.openlca.app;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.openlca.app.rcp.RcpActivator;
import org.openlca.app.util.Numbers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Preferences extends AbstractPreferenceInitializer {

	public static final String NUMBER_ACCURACY = "NUMBER_ACCURACY";
	public static final String LAST_IMPORT_FOLDER = "LAST_IMPORT_FOLDER";
	public static final String LAST_EXPORT_FOLDER = "LAST_EXPORT_FOLDER";

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = getStore();
		store.setDefault(NUMBER_ACCURACY, 5);
	}

	public static void init() {
		Logger log = LoggerFactory.getLogger(Preferences.class);
		log.trace("init preferences");
		IPreferenceStore store = RcpActivator.getDefault().getPreferenceStore();
		int acc = store.getDefaultInt(NUMBER_ACCURACY);
		Numbers.setDefaultAccuracy(acc);
		log.trace("preference {} = {}", NUMBER_ACCURACY, acc);
	}

	public static IPreferenceStore getStore() {
		return RcpActivator.getDefault().getPreferenceStore();
	}

	public static void set(String key, String value) {
		if (key == null)
			return;
		IPreferenceStore store = getStore();
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

}
