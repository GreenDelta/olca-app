package org.openlca.app.collaboration.ui.viewers.json.olca;

import java.util.HashMap;
import java.util.Map;

import org.openlca.app.collaboration.util.Json;
import org.openlca.core.model.AllocationFactor;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.Category;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyType;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.ParameterScope;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.ProjectVariant;
import org.openlca.core.model.RiskLevel;
import org.openlca.core.model.SocialAspect;
import org.openlca.core.model.Uncertainty;
import org.openlca.core.model.UncertaintyType;
import org.openlca.jsonld.Enums;

import com.google.gson.JsonElement;

class EnumFields {

	private static Map<String, Map<String, Class<? extends Enum<?>>>> enums = new HashMap<>();

	static {
		put(FlowProperty.class, "flowPropertyType", FlowPropertyType.class);
		put(Flow.class, "flowType", FlowType.class);
		put(SocialAspect.class, "riskLevel", RiskLevel.class);
		put(Uncertainty.class, "distributionType", UncertaintyType.class);
		put(Parameter.class, "parameterScope", ParameterScope.class);
		put(Process.class, "processType", ProcessType.class);
		put(Process.class, "defaultAllocationMethod", AllocationMethod.class);
		put(AllocationFactor.class, "allocationType", AllocationMethod.class);
		put(ProjectVariant.class, "allocationMethod", AllocationMethod.class);
		put(Category.class, "modelType", ModelType.class);
	}

	private static void put(Class<?> clazz, String property,
			Class<? extends Enum<?>> enumClass) {
		if (!enums.containsKey(clazz.getSimpleName())) {
			enums.put(clazz.getSimpleName(), new HashMap<>());
		}
		enums.get(clazz.getSimpleName()).put(property, enumClass);
	}

	static boolean isEnum(JsonElement element, String property) {
		var type = ModelUtil.getType(element);
		if (type == null)
			return false;
		if (!enums.containsKey(type))
			return false;
		return enums.get(type).containsKey(property);
	}

	static <T extends Enum<T>> T getEnum(JsonElement element, String property) {
		if (!element.isJsonObject())
			return null;
		var value = Json.getString(element, property);
		return getEnum(element, property, value);
	}

	static <T extends Enum<T>> T getEnum(JsonElement element, String property, String label) {
		var type = ModelUtil.getType(element);
		if (type == null)
			return null;
		Class<T> clazz = getEnumType(type, property);
		if (clazz == null)
			return null;
		return Enums.getValue(label, clazz);
	}

	@SuppressWarnings("unchecked")
	private static <T extends Enum<T>> Class<T> getEnumType(String type, String property) {
		if (!enums.containsKey(type))
			return null;
		return (Class<T>) enums.get(type).get(property);
	}

}
