/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.productsystem.graphical.model;

import org.eclipse.draw2d.Border;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.GridData;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.swt.graphics.Color;
import org.openlca.app.Labels;
import org.openlca.app.resources.ImageType;
import org.openlca.core.application.Messages;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;

/**
 * Figure for an {@link ExchangeNode}
 * 
 * @author Sebastian Greve
 * 
 */
public class ExchangeFigure extends Label {

	/**
	 * The background color
	 */
	private static final Color backColor = ColorConstants.white;

	/**
	 * Text color of this label
	 */
	private static final Color textColor = ColorConstants.gray;

	/**
	 * Text color for highlighted label
	 */
	private static final Color textHighlightColor = ColorConstants.lightBlue;

	/**
	 * Height of this label
	 */
	public static int height = 16;
	/**
	 * String for PropertyChangeEvent 'HIGHLIGHT'
	 */
	public static String HIGHLIGHT = "HighlightExchange";

	/**
	 * Indicates if the exchange this figure represents is an avoided product
	 */
	private final boolean avoidedProduct;

	/**
	 * Creates a new instance
	 * 
	 * @param exchangeNode
	 *            The exchange node represented by the figure
	 */
	public ExchangeFigure(final ExchangeNode exchangeNode) {
		final Exchange exchange = exchangeNode.getExchange();
		this.avoidedProduct = exchange.isAvoidedProduct();
		setBorder(new LineBorder(ColorConstants.white, 1));
		setForegroundColor(avoidedProduct ? backColor : textColor);
		setBackgroundColor(backColor);
		setToolTip(new Label(
				(exchange.isAvoidedProduct() ? exchange.getFlow().getFlowType() == FlowType.ProductFlow ? Messages.Systems_AvoidedProductFlow
						: Messages.Systems_AvoidedWasteFlow
						: Labels.flowType(exchange.getFlow()))
						+ ": " + exchangeNode.getName()));
	}

	@Override
	protected void paintFigure(final Graphics graphics) {
		if (avoidedProduct) {
			graphics.drawImage(ImageType.EXCHANGE_BG_LEFT.get(), new Point(
					getLocation().x, getLocation().y));
			for (int i = 5; i < getSize().width - 5; i++) {
				graphics.drawImage((ImageType.EXCHANGE_BG_MIDDLE.get()),
						new Point(getLocation().x + i, getLocation().y));
			}
			graphics.drawImage(ImageType.EXCHANGE_BG_RIGHT.get(), new Point(
					getLocation().x + getSize().width - 5, getLocation().y));
		}
		super.paintFigure(graphics);
	}

	/**
	 * Clears the childrens list
	 */
	public void dispose() {
		getChildren().clear();
	}

	/**
	 * Gets the {@link ProcessFigure} to which this exchange figure belongs
	 * 
	 * @return parent ProcessFigure
	 * 
	 */
	public ProcessFigure getParentProcessFigure() {
		return (ProcessFigure) getParent().getParent();
	}

	/**
	 * set the {@link Color} and the {@link Border} for the given value
	 * 
	 * @param value
	 *            The new value
	 */
	public void setHighlight(final boolean value) {
		if (value) {
			setForegroundColor(textHighlightColor);
		} else {
			setBackgroundColor(backColor);
			setForegroundColor(avoidedProduct ? backColor : textColor);
		}
	}

	/**
	 * Sets the layout for this figure
	 * 
	 * @param layout
	 *            - {@link GridData}
	 */
	public void setLayout(final Object layout) {
		getParent().setConstraint(this, layout);
	}

}
