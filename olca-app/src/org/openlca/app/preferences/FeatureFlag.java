package org.openlca.app.preferences;

/**
 * Feature flags of the application. The flags are stored in the preference
 * store where their names are used as keys.
 */
public enum FeatureFlag {

	TAG_RESULTS("Enable contributions by tags in result views"),

	MATRIX_IMAGE_EXPORT("Enable matrix image export");

	private final String description;

	FeatureFlag(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public boolean isEnabled() {
		return Preferences.getStore().getBoolean(this.name());
	}

}
