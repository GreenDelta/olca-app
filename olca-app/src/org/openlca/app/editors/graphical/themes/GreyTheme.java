package org.openlca.app.editors.graphical.themes;

import org.eclipse.swt.graphics.Color;
import org.openlca.app.editors.graphical.model.ExchangeNode;
import org.openlca.app.editors.graphical.model.Link;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.util.Colors;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.descriptors.ProcessDescriptor;

public class GreyTheme implements Theme{

	private static final Color DARK_GREY = Colors.get(64, 64, 64);
	static final Color LIGHT_GREY = Colors.get(242, 242, 242);
	static final Color WHITE = Colors.white();


	@Override
	public String label() {
		return "Grey";
	}

	@Override
	public String id() {
		return "grey";
	}

	@Override
	public Color ioHeaderForegroundOf(ProcessNode node) {
		return WHITE;
	}
	
	@Override
	public Color graphForeground() {
		return DARK_GREY;
	}
	
	@Override
	public Color graphBorderColor() {
		return DARK_GREY;
	}

	@Override
	public Color ioForegroundOf(ExchangeNode node) {
		return DARK_GREY;
	}

	@Override
	public Color graphBackground() {
		return LIGHT_GREY;
	}

	@Override
	public Color colorOf(Link link) {
		return DARK_GREY;
	}

	@Override
	public Color defaultLinkColor() {
		return DARK_GREY;
	}

	private static boolean isUnitProcess(ProcessNode node) {
		if (node == null || node.process == null)
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
