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
		ModelType[] order = new ModelType[] { ModelType.PROJECT,
				ModelType.PRODUCT_SYSTEM, ModelType.IMPACT_METHOD,
				ModelType.PARAMETER, ModelType.PROCESS, ModelType.FLOW,
				ModelType.COST_CATEGORY, ModelType.SOCIAL_INDICATOR,
				ModelType.FLOW_PROPERTY, ModelType.UNIT_GROUP,
				ModelType.SOURCE, ModelType.ACTOR, ModelType.LOCATION };
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

}
