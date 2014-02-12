package org.openlca.app.rcp.plugins;

/**
 * Even though the Dependency class is a subset of the Plugin class an own class
 * is used to avoid using half filled objects which have to be filled up with
 * information later on. This way it is obvious that the rest of the plugin
 * information is "missing"
 * 
 */
public class Dependency {

	private String symbolicName;

	private String version;

	public String getSymbolicName() {
		return symbolicName;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public void setSymbolicName(String symbolicName) {
		this.symbolicName = symbolicName;
	}

}
