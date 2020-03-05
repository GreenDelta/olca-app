package org.openlca.app.components.mapview;

import org.eclipse.swt.graphics.Color;
import org.openlca.app.util.Colors;
import org.openlca.geo.geojson.Feature;
import org.openlca.geo.geojson.FeatureCollection;

public class LayerConfig {

	final FeatureCollection layer;

	private Color borderColor;
	private Color fillColor;
	private ColorScale fillScale;
	private boolean center;

	private String fillParameter;

	LayerConfig(FeatureCollection layer) {
		this.layer = layer;
	}

	public LayerConfig fillColor(Color c) {
		this.fillColor = c;
		return this;
	}

	public LayerConfig fillScale(String parameter) {
		boolean init = false;
		double min = 0;
		double max = 0;
		for (Feature f : layer.features) {
			if (f.properties == null || f.geometry == null)
				continue;
			Object val = f.properties.get(parameter);
			if (!(val instanceof Number))
				continue;
			double v = ((Number) val).doubleValue();
			if (!init) {
				min = v;
				max = v;
				init = true;
			} else {
				min = Math.min(min, v);
				max = Math.max(max, v);
			}
		}
		return fillScale(parameter, min, max);
	}

	public LayerConfig fillScale(String parameter, double min, double max) {
		this.fillParameter = parameter;
		this.fillScale = new ColorScale(min, max);
		return this;
	}

	public LayerConfig borderColor(Color c) {
		this.borderColor = c;
		return this;
	}

	public LayerConfig center() {
		this.center = true;
		return this;
	}

	// package private getters

	Color getBorderColor() {
		return borderColor != null
				? borderColor
				: Colors.black();
	}

	/**
	 * Returns null when there is no fill color set.
	 */
	Color getFillColor(Feature feature) {
		if (fillScale == null || fillParameter == null)
			return fillColor;
		if (feature == null || feature.properties == null)
			return Colors.darkGray();
		Object val = feature.properties.get(fillParameter);
		if (!(val instanceof Number))
			return Colors.darkGray();
		return fillScale.get(((Number) val).doubleValue());
	}

	boolean isCenter() {
		return center;
	}

	private class ColorScale {

		private final double refVal;

		public ColorScale(double min, double max) {
			refVal = Math.max(Math.abs(min), Math.abs(max));
		}

		public Color get(double val) {
			double share = val / refVal;
			return Colors.getForContribution(share);
		}
	}
}