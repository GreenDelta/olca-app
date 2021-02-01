package org.openlca.app.editors.graphical;

import org.openlca.app.editors.graphical.themes.ColorfulTheme;
import org.openlca.app.editors.graphical.themes.Theme;
import org.openlca.app.editors.graphical.themes.Themes;
import org.openlca.jsonld.Json;

import com.google.gson.JsonObject;

public class GraphConfig {

	public boolean showFlowIcons;
	public boolean showFlowAmounts;
	public boolean showElementaryFlows;
	private Theme theme = new ColorfulTheme();

	/**
	 * Creates a copy from the given configuration.
	 */
	public static GraphConfig from(GraphConfig other) {
		return other == null
			? new GraphConfig()
			: other.clone();
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
	public void applyOn(GraphConfig other) {
		if (other == null)
			return;
		other.showFlowIcons = showFlowIcons;
		other.showFlowAmounts = showFlowAmounts;
		other.showElementaryFlows = showElementaryFlows;
		other.theme = theme;
	}

	@Override
	protected GraphConfig clone() {
		var clone = new GraphConfig();
		clone.showFlowIcons = showFlowIcons;
		clone.showFlowAmounts = showFlowAmounts;
		clone.showElementaryFlows = showElementaryFlows;
		clone.theme = theme;
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
		var themeID = Json.getString(obj, "theme");
		config.theme(Themes.get(themeID));
		return config;
	}

	public JsonObject toJson() {
		var obj = new JsonObject();
		obj.addProperty("showFlowIcons", showFlowIcons);
		obj.addProperty("showFlowAmounts", showFlowAmounts);
		obj.addProperty("showElementaryFlows", showElementaryFlows);
		obj.addProperty("theme", theme().id());
		return obj;
	}
}
