package org.openlca.app.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.openlca.app.rcp.RcpActivator;
import org.openlca.ilcd.io.SodaClient;
import org.openlca.ilcd.io.SodaConnection;

public class IoPreference extends AbstractPreferenceInitializer {

	public static final String ILCD_URL = "ilcd-network-url";
	public static final String ILCD_USER = "ilcd-network-user";
	public static final String ILCD_PASSWORD = "ilcd-network-password";
	public static final String ILCD_LANG = "ilcd-preferred-language";

	@Override
	public void initializeDefaultPreferences() {
		setDefault(ILCD_PASSWORD, "");
		setDefault(ILCD_USER, "user");
		setDefault(ILCD_URL, "http://host-adress.web/path/resource");
		setDefault(ILCD_LANG, Language.getApplicationLanguage().getCode());
	}

	public static String getIlcdPassword() {
		return valueOf(ILCD_PASSWORD);
	}

	public static String getIlcdUser() {
		return valueOf(ILCD_USER);
	}

	public static String getIlcdUrl() {
		return valueOf(ILCD_URL);
	}

	public static String getIlcdLanguage() {
		return valueOf(ILCD_LANG);
	}

	public static SodaClient createClient() {
		SodaConnection con = new SodaConnection();
		con.url = getIlcdUrl();
		con.user = getIlcdUser();
		con.password = getIlcdPassword();
		return new SodaClient(con);
	}

	private static String valueOf(String name) {
		IPreferenceStore store = RcpActivator.getDefault().getPreferenceStore();
		return store.getString(name);
	}

	private static void setDefault(String name, String value) {
		IPreferenceStore store = RcpActivator.getDefault().getPreferenceStore();
		store.setDefault(name, value);
	}

	static void reset() {
		reset(ILCD_URL);
		reset(ILCD_USER);
		reset(ILCD_PASSWORD);
		reset(ILCD_LANG);
	}

	private static void reset(String name) {
		IPreferenceStore store = RcpActivator.getDefault().getPreferenceStore();
		store.setValue(name, store.getDefaultString(name));
	}

}
