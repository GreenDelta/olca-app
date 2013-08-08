package org.openlca.plugin.installer;

import java.util.LinkedList;
import java.util.List;

public class PluginListWrapper {
	private List<Plugin> plugins = new LinkedList<>();

	public List<Plugin> getPlugins() {
		return plugins;
	}

	public void setPlugins(List<Plugin> plugins) {
		this.plugins = plugins;
	}

	@Override
	public String toString() {
		return getPlugins() == null ? "PlugingListWrapper(empty)"
				: getPlugins().toString();
	}

	public Plugin get(Dependency dependency) {
		for (Plugin plugin : getPlugins())
			if (plugin.getSymbolicName().equals(dependency.getSymbolicName()))
				if (plugin.getVersion() == null
						&& dependency.getVersion() == null
						|| plugin.getVersion().equals(dependency.getVersion()))
					return plugin;
		return null;
	}

}
