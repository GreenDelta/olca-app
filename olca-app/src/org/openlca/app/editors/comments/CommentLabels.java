package org.openlca.app.editors.comments;

import java.util.HashMap;
import java.util.Map;

import org.openlca.core.model.ModelType;

class CommentLabels {

	private static final Map<String, String> map = new HashMap<>();

	static {
		map.put("id", "UUID");
		map.put("url", "URL");
		map.put("email", "e-mail");
		map.put("cas", "CAS number");
		map.put("externalFile", "File");
		map.put("isInputParameter", "Type");
		map.put("isQuantitativeReference", "Quantitative reference");
		map.put("refExchange.flow", "Reference product");
		map.put("targetUnit.name", "Target unit");
		map.put("activityUnit.name", "Activity unit");
		map.put("parameterRedefs", "Parameters");
		map.put("isCopyrightProtected", "Copyright protected");
		map.put("exchanges", "Inputs/Outputs");
		map.put("processDocumentation.restrictionsDescription", "Access and use restrictions");
		map.put("processDocumentation.inventoryMethodDescription", "LCI method");
		map.put("processDocumentation.modelingConstantsDescription", "Modeling constants");
		map.put("processDocumentation.completenessDescription", "Data completeness");
		map.put("processDocumentation.dataSelectionDescription", "Data selection");
		map.put("processDocumentation.dataTreatmentDescription", "Data treatment");
		map.put("processDocumentation.samplingDescription", "Sampling procedure");
		map.put("processDocumentation.dataCollectionDescription", "Data collection period");
		map.put("processDocumentation.projectDescription", "Project");
		map.put("dqSystem", "Process data quality scheme");
		map.put("exchangeDqSystem", "Input/Output data quality scheme");
		map.put("targetFlowProperty", "Flow property");
		map.put("targetUnit", "Unit");
		map.put("refExchange.name", "Reference product");
		map.put("impactMethod", "LCIA method");
		map.put("impactCategories.refUnit", "Reference unit");
		map.put("socialDqSystem", "Social data quality scheme");
		map.put("exchanges.dqEntry", "Data quality");
		map.put("exchanges.defaultProvider", "Provider");
		map.put("exchanges.costs", "Costs/Revenue");
		map.put("socialAspects.quality", "Data quality");
		map.put("nwSet", "Normalisation & Weighting set");
		map.put("nwSets", "Normalisation & Weighting sets");
		map.put("product.flow", "Declared product");
		map.put("pcr", "PCR");
		map.put("modules.name", "Module");
		map.put("impactResults.indicator", "Impact category");
	}

	static String get(String path) {
		return get(null, path);
	}

	static String get(ModelType type, String path) {
		if (path == null || path.strip().isEmpty())
			return null;
		if (type == ModelType.CURRENCY && "code".equals(path))
			return "Currency code";
		if (type == ModelType.PROCESS && "dqEntry".equals(path))
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
		if (path == null || path.strip().isEmpty())
			return null;
		if (path.indexOf('.') != -1) {
			path = path.substring(path.lastIndexOf('.') + 1);
		}
		if (path.indexOf('[') != -1) {
			path = path.substring(0, path.lastIndexOf('['));
			if (path.equals("impactCategories")) {
				path = "impactCategory";
			} else if (path.equals("processes")) {
				path = "process";
			} else if (path.equals("flowProperties")) {
				path = "flowProperty";
			} else if (path.charAt(path.length() - 1) == 's') {
				path = path.substring(0, path.length() - 1);
			}
		}
		var result = "";
		for (var index = 0; index < path.length(); index++) {
			var character = path.charAt(index);
			if (index == 0) {
				result += Character.toUpperCase(character);
			} else if (Character.toLowerCase(character) == character) {
				result += character;
			} else {
				result += ' ' + Character.toLowerCase(character);
			}
		}
		return result;
	}

}
