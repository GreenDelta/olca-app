package org.openlca.app.editors.graphical.themes;

import org.eclipse.swt.graphics.Color;
import org.openlca.app.editors.graphical.model.ExchangeNode;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.util.Colors;

/**
 * A Monokai color theme, see: https://monokai.pro/
 */
public class DarkTheme implements Theme {

	private static final Color WHITE = Colors.white();
	private static final Color BLACK = Colors.get(44, 41, 45);
	private static final Color PINK = Colors.get(255, 91, 136);
	private static final Color ORANGE = Colors.get(252, 152, 103);
	private static final Color YELLOW = Colors.get(255, 216, 102);
	private static final Color GREEN = Colors.get(169, 220, 118);
	private static final Color BLUE = Colors.get(120, 220, 232);
	private static final Color LILA = Colors.get(171, 157, 242);

	@Override
	public String label() {
		return "Dark";
	}

	@Override
	public String id() {
		return "dark";
	}

	@Override
	public Color graphBackground() {
		return BLACK;
	}

	@Override
	public Color boxColorOf(ProcessNode node) {
		return BLACK;
	}

	@Override
	public Color boxBorderOf(ProcessNode node) {
		return YELLOW;
	}

	@Override
	public Color boxHeaderBackgroundOf(ProcessNode node) {
		return BLACK;
	}

	@Override
	public Color boxHeaderForegroundOf(ProcessNode node) {
		return BLUE;
	}

	@Override
	public Color ioHeaderForegroundOf(ProcessNode node) {
		return YELLOW;
	}

	@Override
	public Color ioInnerBackgroundOf(ProcessNode node) {
		return BLACK;
	}

	@Override
	public Color ioForegroundOf(ExchangeNode node) {
		return BLUE;
	}

}
