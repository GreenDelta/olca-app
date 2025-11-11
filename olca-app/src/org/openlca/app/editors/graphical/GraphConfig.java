package org.openlca.app.editors.graphical;

import static org.openlca.app.components.graphics.figures.Connection.ROUTERS;
import static org.openlca.app.components.graphics.figures.Connection.ROUTER_CURVE;

import java.util.Objects;

import org.eclipse.draw2d.geometry.Point;
import org.openlca.app.components.graphics.model.Element;
import org.openlca.commons.Copyable;
import org.openlca.jsonld.Json;

import com.google.gson.JsonObject;

public class GraphConfig extends Element implements Copyable<GraphConfig> {

	public static final String CONFIG_PROP = "config";

	private boolean showElementaryFlows = false;
	private String connectionRouter = ROUTER_CURVE;
	private boolean isNodeEditingEnabled = false;
	private double zoom = 1;
	private Point viewLocation = new Point();

	/**
	 * Creates a copy from the given configuration.
	 */
	public static GraphConfig from(GraphConfig other) {
		return other == null
				? new GraphConfig()
				: other.copy();
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
		other.connectionRouter = connectionRouter;
		other.zoom = zoom;
		other.viewLocation = viewLocation;
		other.firePropertyChange(CONFIG_PROP, null, this);
	}

	@Override
	public GraphConfig copy() {
		var clone = new GraphConfig();
		clone.showElementaryFlows = showElementaryFlows;
		clone.isNodeEditingEnabled = isNodeEditingEnabled;
		clone.connectionRouter = connectionRouter;
		clone.zoom = zoom;
		clone.viewLocation = viewLocation;
		return clone;
	}

	public boolean equals(GraphConfig other) {
		if (other == null)
			return false;
		return showElementaryFlows == other.showElementaryFlows
				&& isNodeEditingEnabled == other.isNodeEditingEnabled
				&& Objects.equals(connectionRouter, other.connectionRouter)
				&& zoom == other.zoom
				&& viewLocation.equals(other.viewLocation);
	}

	public static GraphConfig fromJson(JsonObject obj) {
		var config = new GraphConfig();
		if (obj == null)
			return config;
		config.showElementaryFlows = Json.getBool(
				obj, "showElementaryFlows", false);

		var router = Json.getString(obj, "connectionRouter");
		config.connectionRouter = ROUTERS.contains(router) ? router : ROUTER_CURVE;

		config.isNodeEditingEnabled = Json.getBool(
				obj, "isNodeEditingEnabled", false);

		config.zoom = Json.getDouble(obj, "zoom", 1.0);
		config.viewLocation = new Point(
				Json.getInt(obj, "x", 0),
				Json.getInt(obj, "y", 0));
		return config;
	}

	public JsonObject toJson() {
		var obj = new JsonObject();
		obj.addProperty("showElementaryFlows", showElementaryFlows);
		obj.addProperty("connectionRouter", connectionRouter);
		obj.addProperty("isNodeEditingEnabled", isNodeEditingEnabled);
		obj.addProperty("zoom", zoom);
		obj.addProperty("x", viewLocation.x);
		obj.addProperty("y", viewLocation.y);
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

	public void setZoom(double zoom) {
		this.zoom = zoom;
	}

	public void setViewLocation(Point location) {
		viewLocation = location;
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

	public double zoom() {
		return zoom;
	}

	public Point viewLocation() {
		return viewLocation;
	}

}
