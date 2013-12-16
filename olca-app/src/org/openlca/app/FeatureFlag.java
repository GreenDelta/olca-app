package org.openlca.app;

/**
 * Feature flags of the application. The flags are stored in the preference
 * store where their names are used as keys.
 */
public enum FeatureFlag {

	USE_MOZILLA_BROWSER("Use Mozilla browser (not available on every platform)"),

	LOCALISED_LCIA("Enable localised impact assessment"),

	SUNBURST_CHART("Show sunburst chart in analysis"),

	USAGE_MENU(
			"Usage menu in navigation (shows usages of the selected element)"),

	// there are problems with the single precision calculation
	// thus we currently do not support this feature
	USE_SINGLE_PRECISION(
			"Calculation with single precision numbers (requires restart)"),

	USE_SPARSE_MATRICES("Calculate with sparse matrices (requires restart)"),

	PRODUCT_SYSTEM_CUTOFF("Enable cut-offs in product system creations"),

	PRODUCT_SYSTEM_EXPORT("Enable product system excel export"),

	MATRIX_IMAGE_EXPORT("Export a product system matrix as image"),

	AUTOMATIC_UPDATES("Enable automatic updates");

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
