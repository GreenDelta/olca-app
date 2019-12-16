package org.openlca.app.preferences;

import org.openlca.app.Preferences;

/**
 * Feature flags of the application. The flags are stored in the preference
 * store where their names are used as keys.
 */
public enum FeatureFlag {

	MATRIX_IMAGE_EXPORT("Enable matrix image export"),

	FAST_NETWORK_CALCULATION("Enable fast network calculation of processes");
	
	private final String description;

	private FeatureFlag(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public boolean isEnabled() {
		return Preferences.getStore().getBoolean(this.name());
	}

}
