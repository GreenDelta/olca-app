/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.editors.graphical.model;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.swt.graphics.Color;
import org.openlca.app.Messages;
import org.openlca.app.editors.graphical.layout.GraphAnimation;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Labels;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;

class ExchangeFigure extends Label {

	private static final Color BACKGROUND_COLOR = ColorConstants.white;
	private static final Color TEXT_COLOR = ColorConstants.gray;
	private static final Color TEXT_HIGHLIGHTED_COLOR = ColorConstants.lightBlue;

	private ExchangeNode node;

	ExchangeFigure(ExchangeNode node) {
		this.node = node;
		if (node.isDummy())
			return;
		Exchange exchange = node.getExchange();
		setBorder(new LineBorder(ColorConstants.white, 1));
		setForegroundColor(exchange.isAvoidedProduct() ? BACKGROUND_COLOR
				: TEXT_COLOR);
		setBackgroundColor(BACKGROUND_COLOR);
		setToolTip(new Label(getPrefix() + ": " + node.getName()));
	}

	private String getPrefix() {
		if (node.getExchange().isAvoidedProduct())
			if (node.getExchange().getFlow().getFlowType() == FlowType.PRODUCT_FLOW)
				return Messages.Systems_AvoidedProductFlow;
			else
				return Messages.Systems_AvoidedWasteFlow;
		else
			return Labels.flowType(node.getExchange().getFlow());
	}

	void setHighlighted(boolean value) {
		if (value) {
			setForegroundColor(TEXT_HIGHLIGHTED_COLOR);
		} else {
			setBackgroundColor(BACKGROUND_COLOR);
			setForegroundColor(node.getExchange().isAvoidedProduct() ? BACKGROUND_COLOR
					: TEXT_COLOR);
		}
	}

	@Override
	protected void paintFigure(Graphics graphics) {
		if (node.getParent().getParent().isMinimized()
				&& !GraphAnimation.isRunning())
			return;
		if (!node.isDummy() && node.getExchange().isAvoidedProduct()) {
			int x = getLocation().x;
			int y = getLocation().y;
			int width = getSize().width;
			int margin = 5;
			graphics.drawImage(ImageType.EXCHANGE_BG_LEFT.get(), new Point(x,
					y + 2));
			for (int i = margin; i < width - margin; i++)
				graphics.drawImage((ImageType.EXCHANGE_BG_MIDDLE.get()),
						new Point(x + i, y + 2));
			graphics.drawImage(ImageType.EXCHANGE_BG_RIGHT.get(), new Point(x
					+ width - margin, y + 2));
		}
		super.paintFigure(graphics);
	}
}
