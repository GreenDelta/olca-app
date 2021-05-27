package org.openlca.app.editors.graphical.themes;

import org.eclipse.swt.graphics.Color;
import org.openlca.app.editors.graphical.model.Link;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.util.Colors;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.descriptors.ProcessDescriptor;

public class DefaultTheme implements Theme {

	static final Color COLOR_LIGHT_GREY = Colors.get(242, 242, 242);
	static final Color COLOR_WHITE = Colors.white();

	private static final Color COLOR_DARK_GREY = Colors.get(64, 64, 64);

	private static final Color COLOR_PRODUCT = Colors.get(0, 0, 102);
	private static final Color COLOR_WASTE = Colors.get(158, 72, 14);
	private static final Color COLOR_LIBRARY = Colors.get(0, 176, 240);
	private static final Color COLOR_SYSTEM = Colors.get(0, 111, 54);
	private static final Color DEFAULT_BORDER = Colors.get(128, 0, 128);

	private static final Color INFO_COLOR = Colors.get(175, 175, 175);

	@Override
	public String label() {
		return "Default";
	}

	@Override
	public String id() {
		return "default";
	}


	@Override
	public Color defaultFontColor() {
		return Colors.black();
	}

	@Override
	public Color defaultBackgroundColor() {
		return COLOR_WHITE;
	}

	@Override
	public Color defaultBorderColor() {
		return DEFAULT_BORDER;
	}


	@Override
	public Color defaultLinkColor() {
		return COLOR_DARK_GREY;
	}

	@Override
	public Color infoFontColor() {
		return Colors.get(175, 175, 175);
	}

	@Override
	public Color linkColorOf(Link link) {
		if (link == null)
			return COLOR_DARK_GREY;
		var provider = link.provider();
		if (provider == null)
			return COLOR_DARK_GREY;
		return COLOR_DARK_GREY;
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
