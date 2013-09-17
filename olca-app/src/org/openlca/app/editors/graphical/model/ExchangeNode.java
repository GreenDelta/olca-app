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

import org.openlca.app.util.Labels;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.descriptors.Descriptors;

public class ExchangeNode extends Node {

	private Exchange exchange;

	public ExchangeNode(Exchange exchange) {
		this.exchange = exchange;
	}

	public boolean isDummy() {
		return exchange == null;
	}

	@Override
	public InputOutputNode getParent() {
		return (InputOutputNode) super.getParent();
	}

	@Override
	public String getName() {
		if (isDummy())
			return "";
		return Labels.getDisplayName(Descriptors.toDescriptor(exchange
				.getFlow()));
	}

	public Exchange getExchange() {
		return exchange;
	}

	public boolean matches(ExchangeNode node) {
		if (node == null)
			return false;
		if (node.isDummy())
			return false;
		if (isDummy())
			return false;
		if (!exchange.getFlow().equals(node.getExchange().getFlow()))
			return false;
		if (exchange.isInput() == node.getExchange().isInput())
			return false;
		return true;
	}

	public void setHighlighted(boolean value) {
		if (!isDummy())
			((ExchangeFigure) getFigure()).setHighlighted(value);
	}

}
