package org.openlca.app.preferences;

/**
 * Feature flags of the application. The flags are stored in the preference
 * store where their names are used as keys.
 */
public enum FeatureFlag {

	DIRECT_SLCA("New social impact assessment"),

	TAG_RESULTS("Enable contributions by tags in result views"),

	ADDITIONAL_PROPERTIES("Show additional properties of data sets"),

	SD_SIM("Support for system dynamics models");

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
