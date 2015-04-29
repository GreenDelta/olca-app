package org.openlca.app.rcp.plugins;

class Plugin {

	// <json>
	private String symbolicName;
	private String displayName;
	private String logo;
	private String version;
	private String description;
	// </json>

	private boolean restartNecessary;
	private boolean installed = false;
	private boolean updated = false;
	private boolean updateable = false;
	private String currentVersion;

	public String getSymbolicName() {
		return symbolicName;
	}

	public void setSymbolicName(String symbolicName) {
		this.symbolicName = symbolicName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getLogo() {
		return logo;
	}

	public void setLogo(String logo) {
		this.logo = logo;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getFileName() {
		return getSymbolicName() + "_" + getVersion() + ".jar";
	}

	public boolean isRestartNecessary() {
		return restartNecessary;
	}

	public void setRestartNecessary(boolean restartNecessary) {
		this.restartNecessary = restartNecessary;
	}

	public boolean isInstalled() {
		return installed;
	}

	public void setInstalled(boolean installed) {
		this.installed = installed;
	}

	public boolean isUpdated() {
		return updated;
	}

	public void setUpdated(boolean updated) {
		this.updated = updated;
	}

	public boolean isUpdateable() {
		return updateable;
	}

	public void setUpdateable(boolean updateable) {
		this.updateable = updateable;
	}

	public String getCurrentVersion() {
		return currentVersion;
	}

	public void setCurrentVersion(String currentVersion) {
		this.currentVersion = currentVersion;
	}

	public String getFullDisplayName() {
		return getDisplayName() + " " + getVersion();
	}

	@Override
	public String toString() {
		return getSymbolicName() + " (" + getVersion() + ")";
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Plugin))
			return false;
		Plugin plugin = (Plugin) obj;
		if (getSymbolicName().equals(plugin.getSymbolicName()))
			if (getVersion() == null)
				if (plugin.getVersion() == null)
					return true;
				else
					return false;
			else
				return getVersion().equals(plugin);
		return false;
	}
}
