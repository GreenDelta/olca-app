package org.openlca.app.editors.graphical.model;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.layout.Animation;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.core.model.descriptors.Descriptor;

class ExchangeFigure extends Label {

	private static final Color BACKGROUND_COLOR = ColorConstants.white;
	private static final Color TEXT_COLOR = Colors.get(33, 33, 33);
	private static final Color TEXT_HIGHLIGHTED_COLOR = Colors.get(232, 234, 246);

	private final ExchangeNode node;

	ExchangeFigure(ExchangeNode node) {
		this.node = node;
		if (node.isDummy())
			return;
		var exchange = node.exchange;
		setBorder(new LineBorder(ColorConstants.white, 1));
		setForegroundColor(exchange.isAvoided
				? BACKGROUND_COLOR
				: TEXT_COLOR);
		setBackgroundColor(BACKGROUND_COLOR);
		setToolTip(new Label(tooltip()));
		var flowType = node.flowType();
		if (flowType != null && node.config().showFlowIcons) {
			setIcon(Images.get(flowType));
		}
	}

	private String tooltip() {
		var exchange = node.exchange;
		if (exchange == null || exchange.flow == null)
			return "";
		var type = node.flowType();
		var prefix = type == null
				? "?"
				: Labels.of(type);
		if (exchange.isAvoided) {
			prefix += " - avoided";
		}
		var text = prefix + ": " + node.getName() + "\n";
		if (exchange.flow.category != null) {
			text += M.Category + ": " + Labels.getShortCategory(
					Descriptor.of(exchange.flow)) + "\n";
		}
		text += M.Amount + ": "
				+ Numbers.format(exchange.amount)
				+ " " + Labels.name(exchange.unit);
		return text;
	}

	void setHighlighted(boolean value) {
		if (value) {
			setForegroundColor(TEXT_HIGHLIGHTED_COLOR);
		} else {
			setBackgroundColor(BACKGROUND_COLOR);
			setForegroundColor(node.exchange.isAvoided
					? BACKGROUND_COLOR
					: TEXT_COLOR);
		}
	}

	@Override
	protected void paintFigure(Graphics g) {
		if (node.parent().isMinimized() && !Animation.isRunning())
			return;

		if (node.isDummy() || !node.exchange.isAvoided) {
			super.paintFigure(g);
			return;
		}
		int x = getLocation().x;
		int y = getLocation().y;
		int width = getSize().width;
		int margin = 5;
		Image iconLeft = Icon.EXCHANGE_BG_LEFT.get();
		Image iconMiddle = Icon.EXCHANGE_BG_MIDDLE.get();
		Image iconRight = Icon.EXCHANGE_BG_RIGHT.get();
		g.drawImage(iconLeft, new Point(x, y + 2));
		for (int i = margin; i < width - margin; i++)
			g.drawImage(iconMiddle, new Point(x + i, y + 2));
		g.drawImage(iconRight, new Point(x + width - margin, y + 2));
		super.paintFigure(g);
	}
}
