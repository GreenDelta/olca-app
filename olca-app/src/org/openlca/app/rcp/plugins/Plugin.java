package org.openlca.app.rcp.plugins;

import java.util.ArrayList;
import java.util.List;

public class Plugin {

	private String symbolicName;
	private String downloadUrl;
	private String image;
	private String version;
	private String installedVersion;
	private String minOpenLcaVersion;
	private String name;
	private String description;
	private boolean installable;

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public void setInstallable(boolean installable) {
		this.installable = installable;
	}

	public boolean isInstallable() {
		return installable;
	}

	private List<Dependency> dependencies = new ArrayList<>();

	public List<Dependency> getDependencies() {
		return dependencies;
	}

	public void setDependencies(List<Dependency> dependencies) {
		this.dependencies = dependencies;
	}

	public String getSymbolicName() {
		return symbolicName;
	}

	public void setSymbolicName(String symbolicName) {
		this.symbolicName = symbolicName;
	}

	public String getDownloadUrl() {
		return downloadUrl;
	}

	public void setDownloadUrl(String downloadUrl) {
		this.downloadUrl = downloadUrl;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getInstalledVersion() {
		return installedVersion;
	}

	public void setInstalledVersion(String installedVersion) {
		this.installedVersion = installedVersion;
	}

	public String getMinOpenLcaVersion() {
		return minOpenLcaVersion;
	}

	public void setMinOpenLcaVersion(String minOpenLcaVersion) {
		this.minOpenLcaVersion = minOpenLcaVersion;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		return getSymbolicName() + ":(inst:" + getInstalledVersion()
				+ "/avail:" + getVersion() + ")";
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Plugin))
			return false;
		Plugin plugin = (Plugin) obj;
		if (getSymbolicName().equals(plugin.getSymbolicName()))
			if (getVersion() == null && plugin.getVersion() == null
					|| getVersion().equals(plugin))
				return true;
		return false;
	}
}
