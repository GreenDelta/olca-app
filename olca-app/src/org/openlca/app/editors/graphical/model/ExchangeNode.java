package org.openlca.app.editors.graphical.model;

import java.util.Objects;

import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;

public class ExchangeNode extends Node {

	public final Exchange exchange;

	public ExchangeNode(GraphEditor editor, Exchange exchange) {
		super(editor);
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

	public FlowType flowType() {
		return exchange == null || exchange.flow == null
				? null
				: exchange.flow.flowType;
	}

	@Override
	public String getName() {
		if (exchange == null) {
			return "";
		}
		return !editor.config.showFlowAmounts
				? Labels.name(exchange.flow)
				: Numbers.format(exchange.amount, 2)
				+ " " + Labels.name(exchange.unit)
				+ " " + Labels.name(exchange.flow);
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
