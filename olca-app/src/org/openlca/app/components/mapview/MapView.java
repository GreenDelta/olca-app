package org.openlca.app.components.mapview;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.openlca.app.util.Colors;
import org.openlca.geo.calc.Bounds;
import org.openlca.geo.calc.WebMercator;
import org.openlca.geo.geojson.Feature;
import org.openlca.geo.geojson.FeatureCollection;
import org.openlca.geo.geojson.GeoJSON;
import org.openlca.geo.geojson.Geometry;
import org.openlca.geo.geojson.GeometryCollection;
import org.openlca.geo.geojson.LineString;
import org.openlca.geo.geojson.MultiLineString;
import org.openlca.geo.geojson.MultiPoint;
import org.openlca.geo.geojson.MultiPolygon;
import org.openlca.geo.geojson.Point;
import org.openlca.geo.geojson.Polygon;
import org.openlca.util.BinUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapView {

	private final Canvas canvas;
	private final Color white;

	private List<LayerConfig> layers = new ArrayList<>();
	private List<FeatureCollection> projections = new ArrayList<>();

	private final Translation translation = new Translation();
	private int zoom = 0;

	public MapView(Composite parent) {
		this.canvas = new Canvas(parent, SWT.NONE);
		this.white = canvas.getDisplay().getSystemColor(SWT.COLOR_WHITE);
		canvas.addPaintListener(e -> render(e.gc));

		// add mouse listeners
		canvas.addMouseWheelListener(e -> {
			translation.updateCenter(e.x, e.y, zoom);
			if (e.count > 0) {
				zoomIn();
			} else {
				zoomOut();
			}
		});
		canvas.addMouseListener(new DragSupport());
	}

	public void update() {
		canvas.redraw();
		canvas.update();
	}

	public void zoomIn() {
		if (zoom >= 21)
			return;
		zoom += 1;
		projectLayers();
		canvas.redraw();
	}

	public void zoomOut() {
		if (zoom == 0)
			return;
		zoom -= 1;
		projectLayers();
		canvas.redraw();
	}

	private void projectLayers() {
		projections.clear();
		for (LayerConfig config : layers) {
			FeatureCollection projection = WebMercator.project(
					config.layer, zoom);
			projections.add(projection);
		}
	}

	public LayerConfig addLayer(FeatureCollection layer) {
		LayerConfig config = new LayerConfig(layer);
		layers.add(config);
		return config;
	}

	public void removeLayer(LayerConfig config) {
		if (config == null)
			return;
		layers.remove(config);
		projections.clear();
	}

	public void addBaseLayers() {
		Function<String, FeatureCollection> fn = (file) -> {
			try {
				InputStream stream = getClass().getResourceAsStream(file);
				return GeoJSON.unpack(BinUtils.read(stream));
			} catch (Exception e) {
				Logger log = LoggerFactory.getLogger(getClass());
				log.error("failed to add base layer" + file, e);
				return null;
			}
		};
		Color blue = Colors.get(170, 218, 255);
		Color brown = Colors.get(249, 246, 231);
		addLayer(fn.apply("oceans.protopack.gz"))
				.fillColor(blue)
				.borderColor(blue);
		addLayer(fn.apply("land.protopack.gz"))
				.fillColor(brown);
		addLayer(fn.apply("lakes.protopack.gz"))
				.fillColor(blue)
				.borderColor(blue);
		addLayer(fn.apply("countries.protopack.gz"));
	}

	/**
	 * Find an initial zoom and center and calculate the projections.
	 */
	private void initProjection() {
		projections.clear();
		if (layers.isEmpty()) {
			return;
		}

		// find the centered projection
		FeatureCollection ref = null;
		for (LayerConfig config : layers) {
			if (config.isCenter()) {
				ref = config.layer;
				break;
			}
		}

		// TODO: otherwise take the layer
		// with the largest bounds
		if (ref == null) {
			ref = layers.get(0).layer;
		}

		// calculate the center
		Bounds bounds = Bounds.of(ref);
		Point center = bounds.center();
		translation.center.x = center.x;
		translation.center.y = center.y;

		// try to find a good initial zoom
		Rectangle canvSize = canvas.getBounds();
		zoom = 0;
		for (int z = 0; z < 21; z++) {
			Point topLeft = new Point(bounds.minX, bounds.minY);
			Point bottomRight = new Point(bounds.maxX, bounds.maxY);
			WebMercator.apply(topLeft, z);
			WebMercator.apply(bottomRight, z);
			if ((bottomRight.x - topLeft.x) > canvSize.width)
				break;
			if ((bottomRight.y - topLeft.y) > canvSize.height)
				break;
			zoom = z;
		}

		// finally, project the layers
		projectLayers();
	}

	private void render(GC gc) {

		if (projections.size() != layers.size()) {
			initProjection();
		}

		Rectangle canvasSize = canvas.getBounds();
		translation.update(canvasSize, zoom);

		// white background
		gc.setBackground(white);
		gc.fillRectangle(canvasSize);

		if (projections.isEmpty())
			return;

		for (int i = 0; i < projections.size(); i++) {
			LayerConfig config = layers.get(i);
			gc.setForeground(config.getBorderColor());
			FeatureCollection projection = projections.get(i);
			for (Feature f : projection.features) {
				if (!translation.visible(f)) {
					continue;
				}
				render(gc, config, f, f.geometry);
			}
		}
	}

	private void render(GC gc, LayerConfig conf, Feature f, Geometry g) {
		if (g == null)
			return;

		// points
		if (g instanceof Point) {
			renderPoint(gc, conf, f, (Point) g);
			return;
		}
		if (g instanceof MultiPoint) {
			MultiPoint mp = (MultiPoint) g;
			for (Point p : mp.points) {
				renderPoint(gc, conf, f, p);
			}
			return;
		}

		// lines
		if (g instanceof LineString) {
			renderLine(gc, conf, f, (LineString) g);
			return;
		}
		if (g instanceof MultiLineString) {
			MultiLineString ml = (MultiLineString) g;
			for (LineString line : ml.lineStrings) {
				renderLine(gc, conf, f, line);
			}
			return;
		}

		// polygons
		if (g instanceof Polygon) {
			renderPolygon(gc, conf, f, (Polygon) g);
			return;
		}
		if (f.geometry instanceof MultiPolygon) {
			MultiPolygon mp = (MultiPolygon) f.geometry;
			for (Polygon polygon : mp.polygons) {
				renderPolygon(gc, conf, f, polygon);
			}
			return;
		}

		if (g instanceof GeometryCollection) {
			GeometryCollection coll = (GeometryCollection) g;
			for (Geometry gg : coll.geometries) {
				render(gc, conf, f, gg);
			}
		}
	}

	private void renderPolygon(GC gc, LayerConfig conf, Feature f, Polygon polygon) {
		int[] points = translation.translate(polygon);
		Color fillColor = conf.getFillColor(f);
		if (fillColor != null) {
			gc.setBackground(fillColor);
			gc.setAlpha(fillColor.getAlpha());
			gc.fillPolygon(points);
			gc.setAlpha(255);
		}
		// TODO: fill inner rings as white polygons
		// overlapping features can anyhow cause problems
		gc.drawPolygon(points);
	}

	private void renderLine(GC gc, LayerConfig conf, Feature f, LineString line) {
		int[] points = translation.translate(line);
		gc.setLineWidth(5 + zoom);
		gc.drawPolyline(points);
		gc.setLineWidth(1);
	}

	private void renderPoint(GC gc, LayerConfig conf, Feature f, Point point) {
		int[] p = translation.translate(point);
		Color fillColor = conf.getFillColor(f);
		int r = 5 + zoom;
		if (fillColor != null) {
			gc.setBackground(fillColor);
			gc.fillOval(p[0], p[1], r, r);
		} else {
			gc.drawOval(p[0], p[1], r, r);
		}
	}

	/**
	 * Translates between the projection and canvas pixels.
	 */
	private class Translation {

		/**
		 * The translation in x direction: canvas.width / 2 - projectedCenter.x
		 */
		double x;

		/**
		 * The translation in y direction: canvas.height / 2 - projectedCenter.y
		 */
		double y;

		/**
		 * The center of the map in WGS 84 coordinates.
		 */
		final Point center = new Point();

		/**
		 * The projected pixel area that is visible on the canvas.
		 */
		final Bounds view = new Bounds();

		void update(Rectangle canvasSize, int zoom) {
			Point t = center.copy();
			WebMercator.apply(t, zoom);
			double cWidth = canvasSize.width / 2.0;
			double cHeight = canvasSize.height / 2.0;
			x = cWidth - t.x;
			y = cHeight - t.y;
			view.minX = t.x - cWidth;
			view.maxX = t.x + cWidth;
			view.minY = t.y - cHeight;
			view.maxY = t.y + cHeight;
			view.isNil = false;
		}

		void updateCenter(int canvasX, int canvasY, int zoom) {
			Point c = new Point();
			c.x = canvasX - x;
			c.y = canvasY - y;
			WebMercator.inverse(c, zoom);
			center.x = c.x;
			center.y = c.y;
		}

		boolean visible(Feature f) {
			if (f == null || f.geometry == null)
				return false;
			Bounds bounds = Bounds.of(f.geometry);
			return bounds.intersects(view);
		}

		int[] translate(Polygon polygon) {
			if (polygon == null || polygon.rings.size() < 1)
				return new int[0];
			LineString ring = polygon.rings.get(0);
			return translate(ring);
		}

		int[] translate(LineString line) {
			if (line == null)
				return new int[0];
			int[] seq = new int[line.points.size() * 2];
			for (int i = 0; i < line.points.size(); i++) {
				Point p = line.points.get(i);
				seq[2 * i] = (int) (p.x + x);
				seq[2 * i + 1] = (int) (p.y + y);
			}
			return seq;
		}

		int[] translate(Point point) {
			if (point == null)
				return new int[] { 0, 0 };
			return new int[] {
					(int) (point.x + x),
					(int) (point.y + y) };
		}
	}

	private class DragSupport extends MouseAdapter {
		int startX;
		int startY;

		@Override
		public void mouseDown(MouseEvent e) {
			startX = e.x;
			startY = e.y;
			setCursor(SWT.CURSOR_SIZEALL);
			super.mouseDown(e);
		}

		@Override
		public void mouseUp(MouseEvent e) {
			int dx = startX - e.x;
			int dy = startY - e.y;
			if (dx != 0 || dy != 0) {
				Rectangle r = canvas.getBounds();
				translation.updateCenter(
						r.width / 2 + dx,
						r.height / 2 + dy, zoom);
			}
			setCursor(SWT.CURSOR_ARROW);
			canvas.redraw();
		}

		private void setCursor(int c) {
			Display display = canvas.getDisplay();
			canvas.setCursor(display.getSystemCursor(c));
		}
	}
}
