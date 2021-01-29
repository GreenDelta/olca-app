package org.openlca.app.editors.graphical;

import com.google.gson.JsonObject;
import org.openlca.app.editors.graphical.model.ColorfulTheme;
import org.openlca.app.editors.graphical.model.Theme;
import org.openlca.app.editors.graphical.model.WhiteTheme;
import org.openlca.jsonld.Json;

public class GraphConfig {

	public boolean showFlowIcons;
	public boolean showFlowAmounts;
	public boolean showElementaryFlows;
	private Theme theme = new WhiteTheme();

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
	}

	@Override
	protected GraphConfig clone() {
		var clone = new GraphConfig();
		clone.showFlowIcons = showFlowIcons;
		clone.showFlowAmounts = showFlowAmounts;
		clone.showElementaryFlows = showElementaryFlows;
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
		return config;
	}

	public JsonObject toJson() {
		var obj = new JsonObject();
		obj.addProperty("showFlowIcons", showFlowIcons);
		obj.addProperty("showFlowAmounts", showFlowAmounts);
		obj.addProperty("showElementaryFlows", showElementaryFlows);
		return obj;
	}
}
