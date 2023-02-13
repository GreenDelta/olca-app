package org.openlca.app.rcp.images;

enum ModelIconGraph {

	EPD("model/epd_graph.png"),
	FLOW_ELEMENTARY("model/flow_elementary_graph.png"),
	FLOW_PRODUCT("model/flow_product_graph.png"),
	FLOW_WASTE("model/flow_waste_graph.png"),
	PROCESS_GENERIC("model/process_generic_graph.png"),
	PROCESS_PROD("model/process_unit_prod_graph.png"),
	PROCESS_WASTE("model/process_unit_waste_graph.png"),
	PROCESS_SYSTEM_PROD("model/process_system_prod_graph.png"),
	PROCESS_SYSTEM_WASTE("model/process_system_waste_graph.png"),
	RESULT("model/result_graph.png");

	final String fileName;

	ModelIconGraph(String fileName) {
		this.fileName = fileName;
	}

}
