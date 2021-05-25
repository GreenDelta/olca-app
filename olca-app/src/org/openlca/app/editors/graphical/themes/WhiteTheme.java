package org.openlca.app.editors.graphical.themes;

import org.eclipse.swt.graphics.Color;
import org.openlca.app.editors.graphical.model.ExchangeNode;
import org.openlca.app.editors.graphical.model.Link;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.util.Colors;

public class WhiteTheme implements Theme {

	static final Color COLOR_WHITE = Colors.white();
	static final Color COLOR_BLACK = Colors.get(38, 38, 38);

	@Override
	public String label() {
		return "White";
	}

	@Override
	public String id() {
		return "white";
	}

	@Override
	public Color graphBackground() {
		return COLOR_WHITE;
	}

	@Override
	public Color colorOf(Link link) {
		return COLOR_BLACK;
	}

	@Override
	public Color defaultLinkColor() {
		return COLOR_BLACK;
	}
	
	@Override
	public Color graphBorderColor() {
		return COLOR_BLACK;
	}

	@Override
	public Color graphForeground() {
		return COLOR_BLACK;
	}
	
	@Override
	public Color ioHeaderForegroundOf(ProcessNode node) {
		return COLOR_BLACK;
	}

	@Override
	public Color ioForegroundOf(ExchangeNode node) {
		return COLOR_BLACK;
	}
}
