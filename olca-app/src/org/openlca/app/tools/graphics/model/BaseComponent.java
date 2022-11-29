package org.openlca.app.tools.graphics.model;

abstract public class BaseComponent extends Component {

	private double zoom = 1.0;

	public double getZoom() {
		return zoom;
	}

	public void setZoom(double zoom) {
		this.zoom = zoom;
	}

	abstract public Component getFocusComponent();

}
