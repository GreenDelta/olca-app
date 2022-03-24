package org.openlca.app.collaboration.viewers.json.olca;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openlca.app.collaboration.viewers.json.content.IDependencyResolver;
import org.openlca.app.collaboration.viewers.json.content.JsonNode;
import org.openlca.core.model.ModelType;

public class ModelDependencyResolver implements IDependencyResolver {

	public static ModelDependencyResolver INSTANCE = new ModelDependencyResolver();
	private static final EnumMap<ModelType, Map<String, Set<String>>> dependencies = new EnumMap<>(ModelType.class);

	private ModelDependencyResolver() {
	}

	static {
		put(ModelType.PROCESS, "exchanges", "flowProperty", "unit");
		put(ModelType.IMPACT_CATEGORY, "impactFactors", "flowProperty", "unit");
		put(ModelType.SOCIAL_INDICATOR, null, "activityQuantity", "activityUnit");
		put(ModelType.PRODUCT_SYSTEM, null, "refProcess", "refExchange");
		put(ModelType.PRODUCT_SYSTEM, null, "refProcess", "targetFlowProperty");
		put(ModelType.PRODUCT_SYSTEM, null, "refProcess", "targetUnit");
		put(ModelType.PRODUCT_SYSTEM, null, "refExchange", "targetFlowProperty");
		put(ModelType.PRODUCT_SYSTEM, null, "refExchange", "targetUnit");
		put(ModelType.PRODUCT_SYSTEM, null, "targetFlowProperty", "targetUnit");
		put(ModelType.PRODUCT_SYSTEM, null, "processes", "processLinks");
		put(ModelType.PRODUCT_SYSTEM, null, "processes", "parameterSets");
		put(ModelType.PRODUCT_SYSTEM, null, "processLinks", "parameterSets");
		put(ModelType.RESULT, "inputResults", "flowProperty", "unit");		
		put(ModelType.RESULT, "outputResults", "flowProperty", "unit");		
	}

	private static void put(ModelType type, String path, String from, String to) {
		var map = dependencies.computeIfAbsent(type, k -> new HashMap<>());
		var prefix = path != null ? path + "." : "";
		put(map, prefix + from, to);
		put(map, prefix + to, from);
	}

	private static void put(Map<String, Set<String>> map, String from, String to) {
		var values = map.computeIfAbsent(from, k -> new HashSet<>());
		values.add(to);
	}

	@Override
	public Set<String> resolve(JsonNode node) {
		var type = ModelUtil.typeOf(node);
		var map = dependencies.get(type);
		if (map == null)
			return null;
		return map.get(ModelUtil.pathOf(node));
	}

}