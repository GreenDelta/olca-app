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
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.descriptors.Descriptors;

class ExchangeFigure extends Label {

	private static final Color BACKGROUND_COLOR = ColorConstants.white;
	private static final Color TEXT_COLOR = ColorConstants.gray;
	private static final Color TEXT_HIGHLIGHTED_COLOR = ColorConstants.lightBlue;
	private ExchangeNode node;

	ExchangeFigure(ExchangeNode node) {
		this.node = node;
		if (node.isDummy())
			return;
		Exchange exchange = node.exchange;
		setBorder(new LineBorder(ColorConstants.white, 1));
		setForegroundColor(exchange.isAvoided ? BACKGROUND_COLOR : TEXT_COLOR);
		setBackgroundColor(BACKGROUND_COLOR);
		setToolTip(new Label(getToolTipText()));
	}

	private String getToolTipText() {
		String text = getPrefix() + ": " + node.getName() + "\n";
		if (node.exchange.flow == null)
			return text;
		if (node.exchange.flow.category != null) {
			text += M.Category + ": " + Labels.getShortCategory(
					Descriptors.toDescriptor(node.exchange.flow)) + "\n";
		}
		text += M.Amount + ": " + Numbers.format(node.exchange.amount);
		return text;
	}

	private String getPrefix() {
		if (node.exchange.isAvoided) {
			if (node.exchange.flow.flowType == FlowType.PRODUCT_FLOW)
				return M.AvoidedProductFlow;
			return M.AvoidedWasteFlow;
		}
		return Labels.flowType(node.exchange.flow);
	}

	void setHighlighted(boolean value) {
		if (value) {
			setForegroundColor(TEXT_HIGHLIGHTED_COLOR);
		} else {
			setBackgroundColor(BACKGROUND_COLOR);
			setForegroundColor(node.exchange.isAvoided ? BACKGROUND_COLOR : TEXT_COLOR);
		}
	}

	@Override
	protected void paintFigure(Graphics graphics) {
		if (node.parent().isMinimized() && !Animation.isRunning())
			return;
		if (node.isDummy() || !node.exchange.isAvoided) {
			super.paintFigure(graphics);
			return;
		}
		int x = getLocation().x;
		int y = getLocation().y;
		int width = getSize().width;
		int margin = 5;
		Image iconLeft = Icon.EXCHANGE_BG_LEFT.get();
		Image iconMiddle = Icon.EXCHANGE_BG_MIDDLE.get();
		Image iconRight = Icon.EXCHANGE_BG_RIGHT.get();
		graphics.drawImage(iconLeft, new Point(x, y + 2));
		for (int i = margin; i < width - margin; i++)
			graphics.drawImage(iconMiddle, new Point(x + i, y + 2));
		graphics.drawImage(iconRight, new Point(x + width - margin, y + 2));
		super.paintFigure(graphics);
	}
}
