package org.openlca.app.editors.graphical;

import com.google.gson.JsonObject;
import org.openlca.app.tools.graphics.model.Element;
import org.openlca.app.tools.graphics.themes.Theme;
import org.openlca.app.tools.graphics.themes.Themes;
import org.openlca.core.model.Copyable;
import org.openlca.jsonld.Json;

import java.util.Objects;

import static org.openlca.app.tools.graphics.figures.Connection.ROUTER_CURVE;

public class GraphConfig extends Element implements Copyable<GraphConfig> {

	public static final String CONFIG_PROP = "config";

	private boolean showElementaryFlows = false;
	private String connectionRouter = ROUTER_CURVE;
	private boolean isNodeEditingEnabled = false;
	private Theme theme = Themes.getDefault(Themes.MODEL);

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
		other.isNodeEditingEnabled = isNodeEditingEnabled;
		other.theme = theme;
		other.connectionRouter = connectionRouter;
		other.firePropertyChange(CONFIG_PROP, null, this);
	}

	@Override
	public GraphConfig copy() {
		var clone = new GraphConfig();
		clone.showElementaryFlows = showElementaryFlows;
		clone.isNodeEditingEnabled = isNodeEditingEnabled;
		clone.theme = theme;
		clone.connectionRouter = connectionRouter;
		return clone;
	}

	public boolean equals(GraphConfig other) {
		if (other == null)
			return false;
		return showElementaryFlows == other.showElementaryFlows
			&& isNodeEditingEnabled == other.isNodeEditingEnabled
			&& theme.equals(other.theme)
			&& Objects.equals(connectionRouter, other.connectionRouter);
	}

	public static GraphConfig fromJson(JsonObject obj) {
		var config = new GraphConfig();
		if (obj == null)
			return config;
		config.showElementaryFlows = Json.getBool(
			obj, "showElementaryFlows", false);
		config.connectionRouter = Json.getString(
			obj, "connectionRouter");
		config.isNodeEditingEnabled = Json.getBool(
			obj, "isNodeEditingEnabled", false);
		var themeID = Json.getString(obj, "theme");
		config.setTheme(Themes.get(themeID, Themes.MODEL));
		return config;
	}

	public JsonObject toJson() {
		var obj = new JsonObject();
		obj.addProperty("showElementaryFlows", showElementaryFlows);
		obj.addProperty("connectionRouter", connectionRouter);
		obj.addProperty("isNodeEditingEnabled", isNodeEditingEnabled);
		obj.addProperty("theme", getTheme().file());
		return obj;
	}

	public void setShowElementaryFlows(boolean showElementaryFlows) {
		if (showElementaryFlows == this.showElementaryFlows)
			return;
		this.showElementaryFlows = showElementaryFlows;
		firePropertyChange(CONFIG_PROP, null, this);
	}

	public void setConnectionRouter(String router) {
		if (Objects.equals(router, this.connectionRouter))
			return;
		connectionRouter = router;
		firePropertyChange(CONFIG_PROP, null, this);
	}

	public void setNodeEditingEnabled(boolean nodeEditingEnabled) {
		if (nodeEditingEnabled == this.isNodeEditingEnabled)
			return;
		isNodeEditingEnabled = nodeEditingEnabled;
		firePropertyChange(CONFIG_PROP, null, this);
	}

	public boolean showElementaryFlows() {
		return showElementaryFlows;
	}

	public String connectionRouter() {
		return connectionRouter;
	}

	public boolean isNodeEditingEnabled() {
		return isNodeEditingEnabled;
	}

}
