package org.openlca.app.preferencepages;

import org.openlca.app.Preferences;

/**
 * Feature flags of the application. The flags are stored in the preference
 * store where their names are used as keys.
 */
public enum FeatureFlag {

	// the refresh buttons work but currently get always the keyboard focus
	// which looks a bit ugly
	SHOW_REFRESH_BUTTONS("Show refresh buttons in editors"),

	LOCALISED_LCIA("Enable localised impact assessment"),

	EXPERIMENTAL_VISUALISATIONS("Experimental visualisations"),

	USE_SPARSE_MATRICES("Calculate with sparse matrices (requires restart)"),

	MATRIX_IMAGE_EXPORT("Enable matrix image export"),

	// PRODUCT_SYSTEM_CUTOFF("Enable cut-offs in product system creations");

	ECOSPOLD1_EXPORT_CONFIG("EcoSpold 1 export configuration");

	// AUTOMATIC_UPDATES("Enable automatic updates");

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
