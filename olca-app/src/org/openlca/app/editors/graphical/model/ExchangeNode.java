package org.openlca.app.editors.graphical.model;

import java.util.Objects;

import org.openlca.app.util.Labels;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.descriptors.Descriptors;

public class ExchangeNode extends Node {

	public Exchange exchange;

	public ExchangeNode(Exchange exchange) {
		this.exchange = exchange;
	}

	public boolean isDummy() {
		return exchange == null;
	}

	@Override
	public String getName() {
		if (isDummy())
			return "";
		return Labels.getDisplayName(Descriptors.toDescriptor(exchange.flow));
	}

	public boolean matches(ExchangeNode node) {
		if (node == null || this.exchange == null || node.exchange == null)
			return false;
		if (!Objects.equals(exchange.flow, node.exchange.flow))
			return false;
		return exchange.isInput != node.exchange.isInput;
	}

	public void setHighlighted(boolean value) {
		if (isDummy())
			return;
		((ExchangeFigure) figure).setHighlighted(value);
	}

	@Override
	public ProcessNode parent() {
		return (ProcessNode) super.parent().parent();
	}

}
