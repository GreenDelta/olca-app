package org.openlca.app.preferencepages;

import org.openlca.app.Preferences;

/**
 * Feature flags of the application. The flags are stored in the preference
 * store where their names are used as keys.
 */
public enum FeatureFlag {

	// can be removed if it runs stable on Windows 64bit
	// USE_MOZILLA_BROWSER("Use Mozilla browser (not available on every platform)"),

	LOCALISED_LCIA("Enable localised impact assessment"),

	EXPERIMENTAL_VISUALISATIONS("Experimental visualisations"),

	USE_SPARSE_MATRICES("Calculate with sparse matrices (requires restart)"),

	MATRIX_IMAGE_EXPORT("Enable matrix image export"),

	PRODUCT_SYSTEM_CUTOFF("Enable cut-offs in product system creations"),

	REPORTS("Create reports for project results");

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
