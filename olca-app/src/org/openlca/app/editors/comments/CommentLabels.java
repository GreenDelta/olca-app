package org.openlca.app.editors.comments;

import java.util.HashMap;
import java.util.Map;

class CommentLabels {

	private static final Map<String, String> map = new HashMap<>();

	static {
		map.put("id", "UUID");
		map.put("telephone", "Phone");
		map.put("telefax", "Fax");
		map.put("url", "URL");
		map.put("externalFile", "File");
		map.put("inputParameter", "Type");
		map.put("referenceProcess", "Process");
		map.put("referenceExchange", "Product");
		map.put("targetFlowPropertyFactor", "Flow property");
		map.put("targetUnit", "Unit");
		map.put("activityUnit", "Activity unit");
		map.put("parameterRedefs", "Parameters");
		map.put("copyright", "Copyright");
		map.put("exchanges", "Inputs/Outputs");
		map.put("documentation.time", "Time description");
		map.put("documentation.validFrom", "Start date");
		map.put("documentation.validUntil", "End date");
		map.put("documentation.technology", "Technology description");
		map.put("documentation.reviewDetails", "Data set other evaluation");
		map.put("documentation.inventoryMethod", "LCI method");
		map.put("documentation.restrictions", "Access and use restrictions");
		map.put("documentation.sampling", "Sampling procedure");
		map.put("documentation.geography", "Geography description");
		map.put("dqSystem", "Process data quality schema");
		map.put("exchangeDqSystem", "Flow data quality schema");
		map.put("socialDqSystem", "Social data quality schema");
		map.put("nwSet", "Normalisation & Weighting set");
		map.put("casNumber", "CAS number");
	}

	static String get(String path) {
		if ("dqEntry".equals(path))
			return "Data quality entry";
		String mapped = path;
		while (mapped.contains("[")) {
			mapped = mapped.substring(0, mapped.indexOf("[")) + mapped.substring(mapped.indexOf("]") + 1);
		}
		if (map.containsKey(mapped))
			return map.get(mapped);
		return toLabel(path);
	}

	private static String toLabel(String path) {
		if (path == null)
			return null;
		if (path.contains(".")) {
			path = path.substring(path.lastIndexOf(".") + 1);
		}
		if (path.contains("[")) {
			path = path.substring(0, path.lastIndexOf("["));
			path = transformArrayPath(path);
		}
		String result = "";
		for (int i = 0; i < path.length(); i++) {
			char character = path.charAt(i);
			if (i == 0) {
				result += Character.toUpperCase(character);
			} else if (Character.isLowerCase(character)) {
				result += character;
			} else {
				result += " " + Character.toLowerCase(character);
			}
		}
		return result;
	}
	
	private static String transformArrayPath(String path) {
		if ("impactCategories".equals(path)) {
			path = "impactCategory";
		} else if ("processes".equals(path)) {
			path = "process";
		} else if ("flowProperties".equals(path)) {
			path = "flowProperty";
		} else if (path.charAt(path.length() - 1) == 's') {
			path = path.substring(0, path.length() - 1);
		}
		return path;
	}
	
}
