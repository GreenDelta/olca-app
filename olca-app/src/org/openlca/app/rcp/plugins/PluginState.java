package org.openlca.app.rcp.plugins;

import com.google.common.base.Strings;

class PluginState {

	private boolean available = false;
	private boolean installed = false;
	private boolean newerAvailable = false;
	private boolean error = false;

	public static PluginState get(Plugin plugin) {
		PluginState state = new PluginState();
		if (!Strings.isNullOrEmpty(plugin.getVersion()))
			state.available = true;
		if (!Strings.isNullOrEmpty(plugin.getInstalledVersion()))
			state.installed = true;
		if (state.available && state.installed)
			state.newerAvailable = PluginsService.isNewer(plugin.getVersion(),
					plugin.getInstalledVersion());
		return state;
	}

	private PluginState() {
	}

	public boolean isAvailable() {
		return available;
	}

	public boolean isInstalled() {
		return installed;
	}

	public boolean isNewerAvailable() {
		return newerAvailable;
	}

	public boolean isError() {
		return error;
	}

}
