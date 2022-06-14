package org.openlca.app.editors.graphical_legacy;

import org.openlca.app.editors.graphical_legacy.themes.Theme;
import org.openlca.app.editors.graphical_legacy.themes.Themes;
import org.openlca.core.model.Copyable;
import org.openlca.jsonld.Json;

import com.google.gson.JsonObject;

public class GraphConfig implements Copyable<GraphConfig> {

	public boolean showFlowIcons = true;
	public boolean showFlowAmounts = true;
	public boolean showElementaryFlows = false;
	public boolean isRouted = true;
	public boolean isProcessEditingEnabled = false;
	private Theme theme = Themes.getDefault();

	/**
	 * Creates a copy from the given configuration.
	 */
	public static GraphConfig from(GraphConfig other) {
		return other == null
			? new GraphConfig()
			: other.copy();
	}

	public Theme theme() {
		return theme;
	}

	public void theme(Theme theme) {
		if (theme != null) {
			this.theme = theme;
		}
	}

	/**
	 * Copies the settings of this configuration to the
	 * given configuration.
	 */
	public void copyTo(GraphConfig other) {
		if (other == null)
			return;
		other.showFlowIcons = showFlowIcons;
		other.showFlowAmounts = showFlowAmounts;
		other.showElementaryFlows = showElementaryFlows;
		other.isProcessEditingEnabled = isProcessEditingEnabled;
		other.theme = theme;
		other.isRouted = isRouted;
	}

	@Override
	public GraphConfig copy() {
		var clone = new GraphConfig();
		clone.showFlowIcons = showFlowIcons;
		clone.showFlowAmounts = showFlowAmounts;
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
		config.showFlowIcons = Json.getBool(
			obj, "showFlowIcons", false);
		config.showFlowAmounts = Json.getBool(
			obj, "showFlowAmounts", false);
		config.showElementaryFlows = Json.getBool(
			obj, "showElementaryFlows", false);
		config.isRouted = Json.getBool(
				obj, "isRouted", true);
		config.isProcessEditingEnabled = Json.getBool(
			obj, "isProcessEditingEnabled", false);
		var themeID = Json.getString(obj, "theme");
		config.theme(Themes.get(themeID));
		return config;
	}

	public JsonObject toJson() {
		var obj = new JsonObject();
		obj.addProperty("showFlowIcons", showFlowIcons);
		obj.addProperty("showFlowAmounts", showFlowAmounts);
		obj.addProperty("showElementaryFlows", showElementaryFlows);
		obj.addProperty("isRouted", isRouted);
		obj.addProperty("isProcessEditingEnabled", isProcessEditingEnabled);
		obj.addProperty("theme", theme().file());
		return obj;
	}
}
