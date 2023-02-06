package org.openlca.app.rcp.images;

enum ModelSVG {

	EPD("model/epd.svg"),
	FLOW_ELEMENTARY("model/flow_elementary.svg"),
	FLOW_PRODUCT("model/flow_product.svg"),
	FLOW_WASTE("model/flow_waste.svg"),
	PROCESS_GENERIC("model/process_generic.svg"),
	PROCESS_PROD("model/process_unit_prod.svg"),
	PROCESS_WASTE("model/process_unit_waste.svg"),
	PROCESS_SYSTEM_PROD("model/process_system_prod.svg"),
	PROCESS_SYSTEM_WASTE("model/process_system_waste.svg"),
	RESULT("model/result.svg");

	final String fileName;

	ModelSVG(String fileName) {
		this.fileName = fileName;
	}

}
