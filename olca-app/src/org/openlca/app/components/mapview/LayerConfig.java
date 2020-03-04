package org.openlca.app.components.mapview;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.openlca.geo.geojson.Feature;
import org.openlca.geo.geojson.FeatureCollection;

public class LayerConfig {

    final FeatureCollection layer;

    private final Display display;
    private final Color black;
    private final Color grey;

    private Color borderColor;
    private Color fillColor;
    private ColorScale fillScale;
    private boolean center;

    private String fillParameter;

    LayerConfig(Display display, FeatureCollection layer) {
        this.layer = layer;
        this.display = display;
        this.black = display.getSystemColor(SWT.COLOR_BLACK);
        this.grey = display.getSystemColor(SWT.COLOR_GRAY);
    }

    // public configuration function

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
       this.fillScale = new ColorScale(display, min, max);
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
            : black;
    }

    /**
     * Returns null when there is no fill color set.
     */
    Color getFillColor(Feature feature) {
        if (fillScale == null || fillParameter == null)
            return fillColor;
        if (feature == null || feature.properties == null)
            return grey;
        Object val = feature.properties.get(fillParameter);
        if (!(val instanceof Number))
            return grey;
        return fillScale.get(((Number) val).doubleValue());
    }

    boolean isCenter() {
        return center;
    }

}