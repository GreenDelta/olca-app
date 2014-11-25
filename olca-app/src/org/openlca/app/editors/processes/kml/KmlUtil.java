package org.openlca.app.editors.processes.kml;

import java.io.StringReader;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Objects;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.openlca.util.BinUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KmlUtil {

	private KmlUtil() {
	}

	public static byte[] toKmz(String kml) {
		if (kml == null)
			return null;
		if (kml.isEmpty())
			return null;
		try {
			return BinUtils.zip(kml.getBytes("utf-8"));
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(KmlUtil.class);
			log.error("failed to zip KML", e);
			return null;
		}
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

	public static String prettyFormat(String kml) {
		if (kml == null)
			return null;
		try {
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(new StringReader(kml));
			XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
			StringWriter writer = new StringWriter();
			outputter.output(doc, writer);
			return writer.toString();
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(KmlUtil.class);
			log.error("failed to format XML", e);
			return kml;
		}
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
		String type = null;
		if (contains(root, "Polygon"))
			type = "Polygon";
		else if (contains(root, "LineString"))
			type = "Line";
		else if (contains(root, "Point"))
			type = "Point";
		if (type == null)
			return "unknown shape";
		else
			return type + getCoordinates(root);
	}

	private static String getCoordinates(Element root) {
		Element element = findElement(root, "coordinates");
		if (element == null)
			return "";
		String text = element.getTextTrim();
		if (text == null || text.isEmpty())
			return "";
		String[] parts = text.split(" ");
		if (parts.length == 0)
			return "";
		if (parts.length == 1)
			return " [" + formatCoordinate(parts[0]) + "]";
		else
			return " [" + formatCoordinate(parts[0]) + " ... "
					+ formatCoordinate(parts[parts.length - 1]) + "]";
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

	private static boolean contains(Element root, String name) {
		return findElement(root, name) != null;
	}

	private static Element findElement(Element root, String name) {
		if (root == null || name == null)
			return null;
		if (Objects.equals(name, root.getName()))
			return root;
		for (Element child : root.getChildren()) {
			Element found = findElement(child, name);
			if (found != null)
				return found;
		}
		return null;
	}

}
