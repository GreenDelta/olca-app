package org.openlca.app.components.mapview;

import java.util.List;

import org.openlca.geo.geojson.Feature;
import org.openlca.geo.geojson.FeatureCollection;
import org.openlca.geo.geojson.Geometry;
import org.openlca.geo.geojson.GeometryCollection;
import org.openlca.geo.geojson.LineString;
import org.openlca.geo.geojson.MultiLineString;
import org.openlca.geo.geojson.MultiPoint;
import org.openlca.geo.geojson.MultiPolygon;
import org.openlca.geo.geojson.Point;
import org.openlca.geo.geojson.Polygon;

public class Bounds {

    public double minX;
    public double minY;
    public double maxX;
    public double maxY;

    /**
     * Indicates that the values where not initialized.
     */
    public boolean isNil = true;

    public static Bounds of(FeatureCollection coll) {
        if (coll == null)
            return new Bounds();
        return coll.features.parallelStream()
                .map(Bounds::of)
                .reduce(Bounds::join)
                .orElse(new Bounds());
    }

    public static Bounds of(Feature feature) {
        if (feature == null)
            return new Bounds();
        return of(feature.geometry);
    }

    public static Bounds of(Geometry g) {
        Bounds bounds = new Bounds();
        if (g == null)
            return bounds;

        if (g instanceof Point) {
            bounds.addPoint((Point) g);
            return bounds;
        }

        if (g instanceof MultiPoint) {
            bounds.addPoints(((MultiPoint) g).points);
            return bounds;
        }

        if (g instanceof LineString) {
            bounds.addPoints(((LineString) g).points);
            return bounds;
        }

        if (g instanceof MultiLineString) {
            bounds.addLines(((MultiLineString) g).lineStrings);
            return bounds;
        }

        if (g instanceof Polygon) {
            bounds.addLines(((Polygon) g).rings);
            return bounds;
        }

        if (g instanceof MultiPolygon) {
            bounds.addPolygons(((MultiPolygon) g).polygons);
            return bounds;
        }

        if (g instanceof GeometryCollection) {
            return ((GeometryCollection) g).geometries.parallelStream()
                    .map(Bounds::of)
                    .reduce(Bounds::join).orElse(bounds);
        }

        return bounds;
    }

    private static Bounds join(Bounds a, Bounds b) {
        if (a == null && b == null)
            return new Bounds();
        if (a == null || a.isNil)
            return b != null ? b : new Bounds();
        if (b == null || b.isNil)
            return a;

        Bounds joined = new Bounds();
        joined.isNil = false;
        joined.minX = Math.min(a.minX, b.minX);
        joined.minY = Math.min(a.minY, b.minY);
        joined.maxX = Math.max(a.maxX, b.maxX);
        joined.maxY = Math.max(a.maxY, b.maxY);
        return joined;
    }


    private void addPoint(Point p) {
        if (p == null)
            return;

        if (isNil) {
            minX = p.x;
            maxX = p.x;
            minY = p.y;
            maxY = p.y;
            isNil = false;
            return;
        }

        minX = Math.min(p.x, minX);
        maxX = Math.max(p.x, maxX);
        minY = Math.min(p.y, minY);
        maxY = Math.max(p.y, maxY);
    }

    private void addPoints(List<Point> points) {
        if (points == null)
            return;
        for (Point p : points) {
            addPoint(p);
        }
    }

    private void addLines(List<LineString> lines) {
        if (lines == null)
            return;
        for (LineString line : lines) {
            if (line == null)
                continue;
            addPoints(line.points);
        }
    }

    private void addPolygons(List<Polygon> polygons) {
        if (polygons == null)
            return;
        for (Polygon polygon : polygons) {
            if (polygon == null)
                continue;
            addLines(polygon.rings);
        }
    }

    public Point center() {
        Point center = new Point();
        center.x = minX + (maxX - minX) / 2;
        center.y = minY + (maxY - minY) / 2;
        return center;
    }

    public boolean intersects(Bounds other) {
        if (other == null)
            return false;
        // one box is on the left side of the other
        if (maxX < other.minX || other.maxX < minX)
            return false;
        // one bix is under the other
        if (maxY < other.minY || other.maxY < minY)
            return false;
        return true;
    }

    @Override
    public String toString() {
        if (isNil) {
            return "Bounds { nil }";
        }
        return "Bounds { " +
                "(" + minX + ", " + minY + "), ("
                + maxX + ", " + maxY + ") }";
    }
}
