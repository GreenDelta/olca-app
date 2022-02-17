package org.openlca.app.collaboration.viewers.json.olca;

import java.util.HashMap;
import java.util.Map;

import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.FlowPropertyType;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ParameterScope;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.RiskLevel;
import org.openlca.core.model.UncertaintyType;
import org.openlca.jsonld.Enums;

class EnumFields {

	private static Map<String, Class<? extends Enum<?>>> enums = new HashMap<>();

	static {
		enums.put("flowPropertyType", FlowPropertyType.class);
		enums.put("flowType", FlowType.class);
		enums.put("riskLevel", RiskLevel.class);
		enums.put("distributionType", UncertaintyType.class);
		enums.put("parameterScope", ParameterScope.class);
		enums.put("processType", ProcessType.class);
		enums.put("defaultAllocationMethod", AllocationMethod.class);
		enums.put("allocationType", AllocationMethod.class);
		enums.put("allocationMethod", AllocationMethod.class);
		enums.put("modelType", ModelType.class);
	}

	static boolean isEnum(String property) {
		return enums.containsKey(property);
	}

	static <T extends Enum<T>> T getEnum(String property, String value) {
		Class<T> clazz = getEnumType(property);
		if (clazz == null)
			return null;
		return Enums.getValue(value, clazz);
	}

	@SuppressWarnings("unchecked")
	private static <T extends Enum<T>> Class<T> getEnumType(String property) {
		return (Class<T>) enums.get(property);
	}

}
