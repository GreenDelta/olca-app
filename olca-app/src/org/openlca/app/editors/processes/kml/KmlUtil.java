package org.openlca.app.editors.processes.kml;

import java.io.StringReader;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.openlca.app.M;
import org.openlca.geo.kml.FeatureType;
import org.openlca.util.BinUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KmlUtil {

	private KmlUtil() {
	}

	public static String toKml(byte[] kmz) {
		if (kmz == null)
			return null;
		try {
			return new String(BinUtils.unzip(kmz), "utf-8");
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(KmlUtil.class);
			log.error("failed to unzip KMZ", e);
			return null;
		}
	}

	public static String prettyFormat(String kml) throws Exception {
		if (kml == null)
			return null;
		SAXBuilder builder = new SAXBuilder();
		Document doc = builder.build(new StringReader(kml));
		XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
		StringWriter writer = new StringWriter();
		outputter.output(doc, writer);
		return writer.toString();
	}

	public static String getDisplayText(byte[] kmz) {
		String kml = toKml(kmz);
		if (kml == null)
			return "none";
		try {
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(new StringReader(kml));
			return getDisplayText(doc.getRootElement());
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(KmlUtil.class);
			log.error("failed to parse KML", e);
			return "invalid XML";
		}
	}

	private static String getDisplayText(Element root) {
		FeatureType type = null;
		int polygons = count(root, "Polygon");
		int lines = count(root, "LineString");
		int points = count(root, "Point");
		if (polygons > 1)
			type = FeatureType.MULTI_POLYGON;
		else if (polygons > 0)
			type = FeatureType.POLYGON;
		else if (lines > 1)
			type = FeatureType.MULTI_LINE;
		else if (lines > 0)
			type = FeatureType.LINE;
		else if (points > 1)
			type = FeatureType.MULTI_POINT;
		else if (points > 0)
			type = FeatureType.POINT;
		if (type == null)
			return getLabel(type);
		List<Element> elements = findElements(root, "coordinates");
		if (elements.size() == 0)
			return "";
		return getLabel(type) + " [" + getCoordinates(elements, type) + "]";
	}

	private static String getLabel(FeatureType type) {
		switch (type) {
		case POINT:
			return M.Point;
		case LINE:
			return M.Line;
		case POLYGON:
			return M.Polygon;
		case MULTI_POINT:
			return M.MultiPoint;
		case MULTI_LINE:
			return M.MultiLine;
		case MULTI_POLYGON:
			return M.MultiPolygon;
		default:
			return "unsupported shape";
		}
	}

	private static String getCoordinates(List<Element> elements,
			FeatureType type) {
		String[] first = null;
		String[] last = null;
		int count = 0;
		for (Element element : elements) {
			String text = element.getTextTrim();
			if (text == null || text.isEmpty())
				continue;
			String[] parts = text.split(" ");
			if (parts.length == 0)
				continue;
			if (first == null)
				first = parts;
			else
				last = parts;
			count++;
		}
		if (first == null || first.length == 0)
			return "";
		String coords = getCoordinates(first, type);
		if (last == null || last.length == 0)
			return coords;
		coords += " ";
		if (count > 2)
			coords += "... ";
		coords += getCoordinates(last, type);
		return coords;
	}

	private static String getCoordinates(String[] parts, FeatureType type) {
		if (parts == null || parts.length == 0)
			return "";
		String first = formatCoordinate(parts[0]);
		if (type == FeatureType.POINT || type == FeatureType.MULTI_POINT)
			return first;
		if (parts.length < 2)
			return "";
		String last = formatCoordinate(parts[parts.length - 1]);
		if (parts.length == 2)
			return first + " " + last;
		return first + " .. " + last;
	}

	private static String formatCoordinate(String part) {
		try {
			DecimalFormat format = new DecimalFormat("###.00",
					new DecimalFormatSymbols(Locale.US));
			String[] texts = part.split(",");
			double lat = Double.parseDouble(texts[0]);
			double lon = Double.parseDouble(texts[1]);
			return format.format(lat) + "," + format.format(lon);
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(KmlUtil.class);
			log.error("failed to parse coordinate " + part, e);
			return "invalid coordinate";
		}
	}

	private static int count(Element root, String name) {
		return findElements(root, name).size();
	}

	private static List<Element> findElements(Element root, String name) {
		if (root == null || name == null)
			return Collections.emptyList();
		if (Objects.equals(name, root.getName()))
			return Collections.singletonList(root);
		List<Element> elements = new ArrayList<>();
		for (Object obj : root.getChildren()) {
			if (!(obj instanceof Element))
				continue;
			Element child = (Element) obj;
			elements.addAll(findElements(child, name));
		}
		return elements;
	}
}
