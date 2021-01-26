package org.openlca.app.editors.graphical.model;

import org.eclipse.swt.graphics.Color;
import org.openlca.app.util.Colors;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.ProcessDescriptor;

class Figures {

	private static final Color COLOR_DEFAULT = Colors.get(64, 64, 64);
	private static final Color COLOR_PROCESS = Colors.get(0, 0, 102);
	private static final Color COLOR_WASTE_PROCESS = Colors.get(158, 72, 14);
	private static final Color COLOR_LIBRARY_PROCESS = Colors.get(0, 176, 240);
	private static final Color COLOR_PRODUCT_SYSTEM = Colors.get(0, 111, 54);

	static Color colorOf(ProcessNode node) {
		if (node == null || node.process == null)
			return COLOR_DEFAULT;
		var d = node.process;
		if (d.isFromLibrary())
			return COLOR_LIBRARY_PROCESS;
		if (!(d instanceof ProcessDescriptor))
			return COLOR_PRODUCT_SYSTEM;
		// TODO: determine if a process is a waste or production process
		return COLOR_PROCESS;
	}

}
