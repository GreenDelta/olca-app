package org.openlca.app.editors.graphical.themes;

import org.eclipse.swt.graphics.Color;
import org.openlca.app.editors.graphical.model.ExchangeNode;
import org.openlca.app.util.Colors;

public class DraculaTheme implements Theme {

	private static final Color BLACK = Colors.get(40, 42, 54);
	private static final Color WHITE = Colors.get(242, 242, 242);
	private static final Color GRAY = Colors.get(40, 42, 54);
	private static final Color ORANGE = Colors.get(255, 184, 108);
	private static final Color BLUE = Colors.get(139, 233, 253);
	private static final Color GREEN = Colors.get(80, 250, 123);
	private static final Color PINK = Colors.get(255, 121, 198);

	@Override
	public String label() {
		return "Dracula";
	}

	@Override
	public String id() {
		return "dracula";
	}

	@Override
	public Color defaultFontColor() {
		return WHITE;
	}

	@Override
	public Color defaultBackgroundColor() {
		return BLACK;
	}

	@Override
	public Color defaultBorderColor() {
		return PINK;
	}

	@Override
	public Color defaultLinkColor() {
		return WHITE;
	}

	@Override
	public Color infoFontColor() {
		return GRAY;
	}

	@Override
	public Color fontColorOf(ExchangeNode node) {
		if (node == null)
			return defaultFontColor();
		var type = node.flowType();
		if (type == null)
			return defaultFontColor();
		return switch (type) {
			case PRODUCT_FLOW -> BLUE;
			case WASTE_FLOW -> ORANGE;
			case ELEMENTARY_FLOW -> GREEN;
		};
	}
}
