package org.openlca.app.editors.lcia.shapefiles;

import java.awt.Color;

import org.eclipse.swt.graphics.RGB;
import org.geotools.data.DataStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Fill;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.Symbolizer;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Expression;
import org.openlca.app.FaviColor;
import org.openlca.geo.kml.FeatureType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SFMapStyle {

	private SFMapStyle() {
	}

	public static Style create(DataStore dataStore, ShapeFileParameter param,
			FeatureType type) {
		try {
			StyleBuilder styleBuilder = new StyleBuilder();
			Style style = styleBuilder.createStyle();
			double[] breaks = createBreaks(param.min, param.max);
			Color[] colors = createColors();
			Rule[] rules = createRules(param.name, styleBuilder, breaks,
					colors, type);
			String typeName = dataStore.getTypeNames()[0];
			FeatureTypeStyle fts = styleBuilder.createFeatureTypeStyle(
					typeName, rules);
			style.featureTypeStyles().add(fts);
			return style;
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(SFMapStyle.class);
			log.error("failed to create shape file style", e);
			return null;
		}
	}

	private static double[] createBreaks(double min, double max) {
		double step = (max - min) / 9;
		double[] breaks = new double[10];
		for (int i = 0; i < 9; i++) {
			breaks[i] = min + i * step;
		}
		breaks[9] = max;
		return breaks;
	}

	private static Color[] createColors() {
		Color[] colors = new Color[10];
		for (int i = 0; i < 9; i++) {
			double c = i * 1d / 9d;
			colors[i] = createColor(c);
		}
		colors[9] = createColor(1d);
		return colors;
	}

	private static Color createColor(double c) {
		RGB rgb = FaviColor.getForContribution(c);
		return new Color(rgb.red, rgb.green, rgb.blue);
	}

	private static Rule[] createRules(String parameter,
			StyleBuilder styleBuilder, double[] breaks, Color[] colors,
			FeatureType type) {
		FilterFactory filterFactory = CommonFactoryFinder
				.getFilterFactory(GeoTools.getDefaultHints());
		Rule[] rules = new Rule[breaks.length];
		for (int i = 0; i < breaks.length; i++) {
			Symbolizer symbolizer = createSymbolizer(styleBuilder, colors[i],
					type);
			Rule rule = styleBuilder.createRule(symbolizer);
			Expression property = filterFactory.property(parameter);
			Filter filter = filterFactory.lessOrEqual(property,
					filterFactory.literal(breaks[i]));
			if (i > 0) {
				Filter greater = filterFactory.greater(property,
						filterFactory.literal(breaks[i - 1]));
				filter = filterFactory.and(filter, greater);
			}
			rule.setFilter(filter);
			rules[i] = rule;
		}
		return rules;
	}

	private static Symbolizer createSymbolizer(StyleBuilder styleBuilder,
			Color color, FeatureType type) {
		switch (type) {
		case POINT:
			return createPointSymbolizer(styleBuilder);
		case LINE:
			return createLineSymbolizer(styleBuilder, color);
		case POLYGON:
		default:
			return createPolygonSymbolizer(styleBuilder, color);
		}
	}

	private static PointSymbolizer createPointSymbolizer(
			StyleBuilder styleBuilder) {
		return styleBuilder.createPointSymbolizer();
	}

	private static LineSymbolizer createLineSymbolizer(
			StyleBuilder styleBuilder, Color color) {
		return styleBuilder.createLineSymbolizer(color, 2);
	}

	private static PolygonSymbolizer createPolygonSymbolizer(
			StyleBuilder styleBuilder, Color color) {
		Fill fill = styleBuilder.createFill(color);
		Stroke stroke = styleBuilder.createStroke(color, 1d);
		return styleBuilder.createPolygonSymbolizer(stroke, fill);
	}

}
