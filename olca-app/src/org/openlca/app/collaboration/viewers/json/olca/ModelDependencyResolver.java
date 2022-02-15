package org.openlca.app.collaboration.viewers.json.olca;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openlca.app.collaboration.viewers.json.content.IDependencyResolver;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.ImpactFactor;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.SocialIndicator;

import com.google.gson.JsonElement;

public class ModelDependencyResolver implements IDependencyResolver {

	public static ModelDependencyResolver INSTANCE = new ModelDependencyResolver();
	private static final Map<String, Map<String, Set<String>>> dependencies = new HashMap<>();

	private ModelDependencyResolver() {
	}

	static {
		put(Exchange.class, "flowProperty", "unit");
		put(ImpactFactor.class, "flowProperty", "unit");
		put(SocialIndicator.class, "activityQuantity", "activityUnit");
		put(ProductSystem.class, "referenceProcess", "referenceExchange");
		put(ProductSystem.class, "referenceExchange", "targetFlowProperty");
		put(ProductSystem.class, "targetFlowProperty", "targetUnit");
		put(ProductSystem.class, "processes", "processLinks");
		put(ProductSystem.class, "processes", "parameterSets");
		put(ProductSystem.class, "processLinks", "parameterSets");
	}

	private static void put(Class<?> clazz, String from, String to) {
		var map = dependencies.computeIfAbsent(clazz.getSimpleName(), k -> new HashMap<>());
		put(map, from, to);
		put(map, to, from);
	}

	private static void put(Map<String, Set<String>> map, String from, String to) {
		var values = map.computeIfAbsent(from, k -> new HashSet<>());
		values.add(to);
	}

	@Override
	public Set<String> resolve(JsonElement parent, String property) {
		var type = ModelUtil.getType(parent);
		if (type == null)
			return null;
		var map = dependencies.get(type);
		if (map == null)
			return null;
		return map.get(property);
	}

}