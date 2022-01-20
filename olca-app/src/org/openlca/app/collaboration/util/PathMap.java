package org.openlca.app.collaboration.util;

import java.util.HashMap;
import java.util.Map;

class PathMap {

	private static final Map<String, String> map = new HashMap<>();

	static {
		// ProductSystem
		map.put("targetFlowPropertyFactor", "targetFlowProperty");
		// ProcessDocumentation
		map.put("documentation.time", "processDocumentation.timeDescription");
		map.put("documentation.technology", "processDocumentation.technologyDescription");
		map.put("documentation.dataCollectionPeriod", "processDocumentation.dataCollectionDescription");
		map.put("documentation.completeness", "processDocumentation.completenessDescription");
		map.put("documentation.dataSelection", "processDocumentation.dataSelectionDescription");
		map.put("documentation.dataTreatment", "processDocumentation.dataTreatmentDescription");
		map.put("documentation.inventoryMethod", "processDocumentation.inventoryMethodDescription");
		map.put("documentation.modelingConstants", "processDocumentation.modelingConstantsDescription");
		map.put("documentation.sampling", "processDocumentation.samplingDescription");
		map.put("documentation.restrictions", "processDocumentation.restrictionsDescription");
		map.put("documentation.project", "processDocumentation.projectDescription");
		map.put("documentation.geography", "processDocumentation.geographyDescription");
		// Flow
		map.put("casNumber", "cas");
	}

	static String get(String path) {
		if (map.containsKey(path))
			return map.get(path);
		if (path.startsWith("documentation."))
			path = "processDocumentation." + path.substring(path.indexOf(".") + 1);
		return path;
	}

}
