package org.openlca.app.editors.graphical.model;

import java.util.Objects;

import org.openlca.app.util.Labels;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.descriptors.Descriptors;

public class ExchangeNode extends Node {

	public final Exchange exchange;

	public ExchangeNode(Exchange exchange) {
		this.exchange = exchange;
	}

	/**
	 * Dummy nodes are created to have the same size of inputs and outputs in a
	 * process figure. The exchange is null in this case.
	 */
	public boolean isDummy() {
		return exchange == null;
	}

	public boolean isWaste() {
		if (exchange == null || exchange.flow == null)
			return false;
		return exchange.flow.flowType == FlowType.WASTE_FLOW;
	}

	@Override
	public String getName() {
		if (exchange == null)
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
