package org.openlca.app.editors.graphical.themes;

import org.eclipse.swt.graphics.Color;
import org.openlca.app.editors.graphical.model.Link;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.util.Colors;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.descriptors.ProcessDescriptor;

public class DefaultTheme implements Theme {

	private final Color SUB_SYSTEM_COLOR = Colors.get(18, 89, 133);
	private final Color PROCESS_COLOR = Colors.get(128, 0, 128);
	private final Color LIBRARY_COLOR = Colors.get(255, 151, 0);
	private static final Color INFO_COLOR = Colors.get(175, 175, 175);

	static final Color WHITE = Colors.white();
	private static final Color DARK_GREY = Colors.get(64, 64, 64);


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
		return WHITE;
	}

	@Override
	public Color defaultBorderColor() {
		return PROCESS_COLOR;
	}


	@Override
	public Color defaultLinkColor() {
		return DARK_GREY;
	}

	@Override
	public Color infoFontColor() {
		return INFO_COLOR;
	}

	@Override
	public Color linkColorOf(Link link) {
		if (link == null)
			return DARK_GREY;
		var provider = link.provider();
		if (provider == null)
			return DARK_GREY;
		return DARK_GREY;
	}

	@Override
	public Color borderColorOf(ProcessNode node) {
		if (node == null || node.process == null)
			return defaultBorderColor();
		if (node.process.isFromLibrary())
			return LIBRARY_COLOR;
		if (!(node.process instanceof ProcessDescriptor))
			return SUB_SYSTEM_COLOR;
		return PROCESS_COLOR;
	}
}
