package org.openlca.core.application;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.openlca.app.plugin.Activator;
import org.openlca.core.math.MatrixFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Preferences extends AbstractPreferenceInitializer {

	public static final String NUMBER_ACCURACY = "NUMBER_ACCURACY";

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = getStore();
		store.setDefault(NUMBER_ACCURACY, 5);
	}

	public static void init() {
		Logger log = LoggerFactory.getLogger(Preferences.class);
		log.trace("init preferences");
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		int acc = store.getDefaultInt(NUMBER_ACCURACY);
		Numbers.setDefaultAccuracy(acc);
		log.trace("preference {} = {}", NUMBER_ACCURACY, acc);
		new ColorInit().run();

		// if (FeatureFlag.USE_SINGLE_PRECISION.isEnabled())
		MatrixFactory.configure(MatrixFactory.PREFER_DOUBLES);
	}

	public static IPreferenceStore getStore() {
		return Activator.getDefault().getPreferenceStore();
	}

}
