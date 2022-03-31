package org.openlca.app.rcp.images;

enum ModelIcon {

	ACTOR("model/actor.png"),
	ACTOR_CATEGORY("category/actor.png"),
	ACTOR_WIZARD("wizard/actor.png"),

	CURRENCY("model/currency.png"),
	CURRENCY_CATEGORY("category/currency.png"),
	CURRENCY_WIZARD("wizard/currency.png"),

	DQ_SYSTEM("model/data_quality_system.png"),
	DQ_SYSTEM_CATEGORY("category/data_quality_system.png"),
	DQ_SYSTEM_WIZARD("wizard/data_quality_system.png"),

	EPD("model/epd.png"),
	EPD_CATEGORY("category/epd.png"),
	EPD_WIZARD("wizard/epd.png"),

	FLOW("model/flow.png"),
	FLOW_ELEMENTARY("model/flow_elementary.png"),
	FLOW_PRODUCT("model/flow_product.png"),
	FLOW_WASTE("model/flow_waste.png"),
	FLOW_CATEGORY("category/flow.png"),
	FLOW_WIZARD("wizard/flow.png"),

	FLOW_PROPERTY("model/flow_property.png"),
	FLOW_PROPERTY_CATEGORY("category/flow_property.png"),
	FLOW_PROPERTY_WIZARD("wizard/flow_property.png"),

	GROUP_BACKGROUND_DATA("model/group_background_data.png"),
	GROUP_INDICATORS("model/group_indicators.png"),
	GROUP_INVENTORY("model/group_inventory.png"),
	GROUP_MODELS("model/group_models.png"),

	IMPACT_CATEGORY("model/impact_category.png"),
	IMPACT_CATEGORY_CATEGORY("category/impact_category.png"),
	IMPACT_CATEGORY_WIZARD("wizard/impact_category.png"),

	IMPACT_METHOD("model/impact_method.png"),
	IMPACT_METHOD_CATEGORY("category/impact_method.png"),
	IMPACT_METHOD_WIZARD("wizard/impact_method.png"),

	LOCATION("model/location.png"),
	LOCATION_CATEGORY("category/location.png"),
	LOCATION_WIZARD("wizard/location.png"),

	PARAMETER("model/parameter.png"),
	PARAMETER_CATEGORY("category/parameter.png"),
	PARAMETER_WIZARD("wizard/parameter.png"),

	PROCESS("model/process.png"),
	PROCESS_SYSTEM("model/process_system.png"),
	PROCESS_CATEGORY("category/process.png"),
	PROCESS_WIZARD("wizard/process.png"),

	PRODUCT_SYSTEM("model/product_system.png"),
	PRODUCT_SYSTEM_CATEGORY("category/product_system.png"),
	PRODUCT_SYSTEM_WIZARD("wizard/product_system.png"),

	PROJECT("model/project.png"),
	PROJECT_CATEGORY("category/project.png"),
	PROJECT_WIZARD("wizard/project.png"),

	SOCIAL_INDICATOR("model/social_indicator.png"),
	SOCIAL_INDICATOR_CATEGORY("category/social_indicator.png"),
	SOCIAL_INDICATOR_WIZARD("wizard/social_indicator.png"),

	SOURCE("model/source.png"),
	SOURCE_CATEGORY("category/source.png"),
	SOURCE_WIZARD("wizard/source.png"),

	RESULT("model/result.png"),
	RESULT_CATEGORY("category/result.png"),
	RESULT_WIZARD("wizard/result.png"),

	UNIT_GROUP("model/unit_group.png"),
	UNIT_GROUP_CATEGORY("category/unit_group.png"),
	UNIT_GROUP_WIZARD("wizard/unit_group.png");

	final String fileName;

	ModelIcon(String fileName) {
		this.fileName = fileName;
	}

}
