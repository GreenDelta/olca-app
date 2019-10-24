package org.openlca.app.navigation;

import java.util.HashMap;
import java.util.Map;

import org.openlca.core.model.ModelType;

public class ModelTypeComparison {

	private static Map<ModelType, Integer> typeOrder = new HashMap<>();

	static {
		fillTypeOrder();
	}

	private static void fillTypeOrder() {
		ModelType[] order = getOrderedTypes();
		for (int i = 0; i < order.length; i++)
			typeOrder.put(order[i], i);
	}

	public static int compare(ModelType type1, ModelType type2) {
		if (type1 == null || type2 == null)
			return 0;
		Integer order1 = typeOrder.get(type1);
		Integer order2 = typeOrder.get(type2);
		if (order1 == null || order2 == null)
			return 0;
		return order1 - order2;
	}

	public static ModelType[] getOrderedTypes() {
		return new ModelType[] {
				ModelType.CATEGORY,
				ModelType.PROJECT,
				ModelType.PRODUCT_SYSTEM,
				ModelType.IMPACT_METHOD,
				ModelType.IMPACT_CATEGORY,
				ModelType.PROCESS,
				ModelType.FLOW,
				ModelType.SOCIAL_INDICATOR,
				ModelType.PARAMETER,
				ModelType.FLOW_PROPERTY,
				ModelType.UNIT_GROUP,
				ModelType.CURRENCY,
				ModelType.ACTOR,
				ModelType.SOURCE,
				ModelType.LOCATION,
				ModelType.DQ_SYSTEM
		};
	}

}
