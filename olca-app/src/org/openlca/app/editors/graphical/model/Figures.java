package org.openlca.app.editors.graphical.model;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Colors;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.descriptors.ProcessDescriptor;

class Figures {

	static final Color COLOR_LIGHT_GREY = Colors.get(242, 242, 242);
	static final Color COLOR_WHITE = Colors.white();

	private static final Color COLOR_DARK_GREY = Colors.get(64, 64, 64);

	private static final Color COLOR_PRODUCT = Colors.get(0, 0, 102);
	private static final Color COLOR_WASTE = Colors.get(158, 72, 14);
	private static final Color COLOR_LIBRARY = Colors.get(0, 176, 240);
	private static final Color COLOR_SYSTEM = Colors.get(0, 111, 54);


	static Color colorOf(ProcessNode node) {
		if (node == null || node.process == null)
			return COLOR_DARK_GREY;
		var d = node.process;
		if (d.isFromLibrary())
			return COLOR_LIBRARY;
		if (!(d instanceof ProcessDescriptor))
			return COLOR_SYSTEM;
		// TODO: determine if a process is a waste or production process
		return COLOR_PRODUCT;
	}

	static Color colorOf(ExchangeNode node) {
		if (node == null)
			return COLOR_DARK_GREY;
		var flowType = node.flowType();
		if (flowType == null)
			return COLOR_DARK_GREY;
		switch (flowType) {
			case PRODUCT_FLOW:
				return COLOR_PRODUCT;
			case WASTE_FLOW:
				return COLOR_WASTE;
			default:
				return COLOR_DARK_GREY;
		}
	}


	static Image iconOf(ProcessNode node) {
		return Icon.GRAPH_PROCESS_PRODUCTION.get();
	}

	static Color headerBackgroundOf(ProcessNode node) {
		return isUnitProcess(node)
			? COLOR_LIGHT_GREY
			: colorOf(node);
	}

	static Color headerForegroundOf(ProcessNode node) {
		return isUnitProcess(node)
			? colorOf(node)
			: COLOR_WHITE;
	}

	private static boolean isUnitProcess(ProcessNode node) {
		if (node == null)
			return false;
		var d = node.process;
		if (d.isFromLibrary())
			return false;
		if (!(d instanceof ProcessDescriptor))
			return false;
		var p = (ProcessDescriptor) d;
		return p.processType == ProcessType.UNIT_PROCESS;
	}

}
