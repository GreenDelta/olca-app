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

// see https://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
class WebMercator {

    private WebMercator() {
    }

    /**
     * Projects a WGS 84 (longitude, latitude)-point to a (x,y)- pixel
     * coordinate. It directly mutates the coordinates of the point.
     */
    static void project(Point p, int zoom) {
        if (p == null)
            return;
        double lon = p.x;
        if (lon < -180) {
            lon = -180;
        } else if (lon > 180) {
            lon = 180;
        }
        double lat = p.y;
        if (lat < -85.0511) {
            lat = -85.0511;
        } else if (lat > 85.0511) {
            lat = 85.0511;
        }

        lon *= Math.PI / 180;
        lat *= Math.PI / 180;
        double scale = (256 / (2 * Math.PI)) * Math.pow(2, zoom);
        p.x = scale * (lon + Math.PI);
        p.y = scale * (Math.PI - Math.log(Math.tan(Math.PI / 4 + lat / 2)));
    }

    /**
     * The inverse operation of project. Calculates a WGS 84 (longitude,
     * latitude)-point from a pixel coordinate. It directly mutates the
     * given point.
     */
    static void unproject(Point p, int zoom) {
        if (p == null)
            return;
        double scale = (256 / (2 * Math.PI)) * Math.pow(2, zoom);
        p.x = (p.x / scale) - Math.PI;
        p.y = 2 * Math.atan(Math.exp(Math.PI - p.y / scale)) - Math.PI / 2;
        p.x *= 180 / Math.PI;
        p.y *= 180 / Math.PI;
    }

    static Geometry apply(Geometry geometry, int zoom) {
        if (geometry == null)
            return null;
        Geometry g = geometry.clone();
        onGeometry(g, zoom);
        return g;
    }

    static Feature apply(Feature feature, int zoom) {
        if (feature == null)
            return null;
        Feature f = feature.clone();
        if (f.geometry != null) {
            onGeometry(f.geometry, zoom);
        }
        return f;
    }

    static FeatureCollection apply(FeatureCollection coll, int zoom) {
        if (coll == null)
            return null;
        FeatureCollection c = coll.clone();
        for (Feature f : c.features) {
            onGeometry(f.geometry, zoom);
        }
        return c;
    }

    private static void onGeometry(Geometry g, int zoom) {
        if (g == null)
            return;

        if (g instanceof Point) {
            project((Point) g, zoom);
            return;
        }

        if (g instanceof MultiPoint) {
            onPoints(((MultiPoint) g).points, zoom);
            return;
        }

        if (g instanceof LineString) {
            onPoints(((LineString) g).points, zoom);
            return;
        }

        if (g instanceof MultiLineString) {
            onLines(((MultiLineString) g).lineStrings, zoom);
            return;
        }

        if (g instanceof Polygon) {
            onLines(((Polygon) g).rings, zoom);
            return;
        }

        if (g instanceof MultiPolygon) {
            onPolygons(((MultiPolygon) g).polygons, zoom);
            return;
        }

        if (g instanceof GeometryCollection) {
            GeometryCollection coll = (GeometryCollection) g;
            for (Geometry cg : coll.geometries) {
                onGeometry(cg, zoom);
            }
        }
    }

    private static void onPoints(List<Point> points, int zoom) {
        if (points == null)
            return;
        for (Point p : points) {
            if (p != null) {
                project(p, zoom);
            }
        }
    }

    private static void onLines(List<LineString> lines, int zoom) {
        if (lines == null)
            return;
        for (LineString line : lines) {
            if (line == null)
                continue;
            onPoints(line.points, zoom);
        }
    }

    private static void onPolygons(List<Polygon> polygons, int zoom) {
        if (polygons == null)
            return;
        for (Polygon p : polygons) {
            if (p == null)
                continue;
            onLines(p.rings, zoom);
        }
    }
}
