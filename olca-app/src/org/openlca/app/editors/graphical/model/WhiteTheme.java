package org.openlca.app.editors.graphical.model;

import org.eclipse.swt.graphics.Color;
import org.openlca.app.util.Colors;

public class WhiteTheme implements Theme {

	static final Color COLOR_WHITE = Colors.white();
	static final Color COLOR_BLACK = Colors.get(38, 38, 38);

	@Override
	public Color boxColorOf(ProcessNode node) {
		return COLOR_WHITE;
	}

	@Override
	public Color boxBorderOf(ProcessNode node) {
		return COLOR_BLACK;
	}

	@Override
	public Color boxHeaderForegroundOf(ProcessNode node) {
		return COLOR_BLACK;
	}

	@Override
	public Color boxHeaderBackgroundOf(ProcessNode node) {
		return COLOR_WHITE;
	}

	@Override
	public Color ioHeaderForegroundOf(ProcessNode node) {
		return COLOR_BLACK;
	}

	@Override
	public Color ioInnerBackgroundOf(ProcessNode node) {
		return COLOR_WHITE;
	}

	@Override
	public Color ioForegroundOf(ExchangeNode node) {
		return COLOR_BLACK;
	}
}
