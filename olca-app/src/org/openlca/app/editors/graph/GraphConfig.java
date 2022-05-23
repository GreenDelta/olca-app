package org.openlca.app.editors.graph;

import com.google.gson.JsonObject;
import org.openlca.app.editors.graph.model.GraphElement;
import org.openlca.app.editors.graph.themes.Theme;
import org.openlca.app.editors.graph.themes.Themes;
import org.openlca.core.model.Copyable;
import org.openlca.jsonld.Json;

public class GraphConfig extends GraphElement implements Copyable<GraphConfig> {

	public static final String CONFIG_PROP = "theme";

	private boolean showElementaryFlows = false;
	private boolean isRouted = true;
	private boolean isProcessEditingEnabled = false;
	private Theme theme = Themes.getDefault();

	/**
	 * Creates a copy from the given configuration.
	 */
	public static GraphConfig from(GraphConfig other) {
		return other == null
			? new GraphConfig()
			: other.copy();
	}

	public Theme getTheme() {
		return theme;
	}

	public void setTheme(Theme theme) {
		if (theme != null) {
			this.theme = theme;
			firePropertyChange(CONFIG_PROP, null, theme);
		}
	}

	/**
	 * Copies the settings of this configuration to the
	 * given configuration.
	 */
	public void copyTo(GraphConfig other) {
		if (other == null)
			return;
		other.showElementaryFlows = showElementaryFlows;
		other.isProcessEditingEnabled = isProcessEditingEnabled;
		other.theme = theme;
		other.isRouted = isRouted;
		other.firePropertyChange(CONFIG_PROP, null, this);
	}

	@Override
	public GraphConfig copy() {
		var clone = new GraphConfig();
		clone.showElementaryFlows = showElementaryFlows;
		clone.isProcessEditingEnabled = isProcessEditingEnabled;
		clone.theme = theme;
		clone.isRouted = isRouted;
		return clone;
	}

	public static GraphConfig fromJson(JsonObject obj) {
		var config = new GraphConfig();
		if (obj == null)
			return config;
		config.showElementaryFlows = Json.getBool(
			obj, "showElementaryFlows", false);
		config.isRouted = Json.getBool(
				obj, "isRouted", true);
		config.isProcessEditingEnabled = Json.getBool(
			obj, "isProcessEditingEnabled", false);
		var themeID = Json.getString(obj, "theme");
		config.setTheme(Themes.get(themeID));
		return config;
	}

	public JsonObject toJson() {
		var obj = new JsonObject();
		obj.addProperty("showElementaryFlows", showElementaryFlows);
		obj.addProperty("isRouted", isRouted);
		obj.addProperty("isProcessEditingEnabled", isProcessEditingEnabled);
		obj.addProperty("theme", getTheme().file());
		return obj;
	}

	public void setShowElementaryFlows(boolean showElementaryFlows) {
		if (showElementaryFlows == this.showElementaryFlows)
			return;
		this.showElementaryFlows = showElementaryFlows;
		firePropertyChange(CONFIG_PROP, null, this);
	}

	public void setRouted(boolean routed) {
		if (routed == this.isRouted)
			return;
		isRouted = routed;
		firePropertyChange(CONFIG_PROP, null, this);
	}

	public void setProcessEditingEnabled(boolean processEditingEnabled) {
		if (processEditingEnabled == this.isProcessEditingEnabled)
			return;
		isProcessEditingEnabled = processEditingEnabled;
		firePropertyChange(CONFIG_PROP, null, this);
	}

	public boolean showElementaryFlows() {
		return showElementaryFlows;
	}

	public boolean isRouted() {
		return isRouted;
	}

	public boolean isProcessEditingEnabled() {
		return isProcessEditingEnabled;
	}

}
