package org.openlca.app.editors.graphical.model;

import org.openlca.app.tools.graphics.model.Component;
import org.openlca.app.util.Labels;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ProcessLink;

import java.util.Objects;

public class ExchangeItem extends Component {

	public final Exchange exchange;

	public ExchangeItem(Exchange exchange) {
		this.exchange = exchange;
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

	public boolean isRefFlow() {
		if (exchange == null)
			return false;
		var parent = getNode();
		if (parent == null)
			return false;
		return parent.getRefFlow() != null
				&& parent.getRefFlow().equals(exchange.flow);
	}

	public Graph getGraph() {
		return getIOPane().getGraph();
	}

	public IOPane getIOPane() {
		return (IOPane) getParent();
	}

	public Node getNode() {
		return getIOPane().getNode();
	}

	public Exchange getExchange() {
		return exchange;
	}

	/**
	 * Returns true if this exchange is connected.
	 */
	public boolean isConnected() {
		var node = getNode();
		if (node == null || node.descriptor == null || exchange == null)
			return false;
		var linkSearch = node.getGraph().linkSearch;
		var links = linkSearch.getConnectionLinks(node.descriptor.id);
		for (ProcessLink link : links) {
			if (link.exchangeId == exchange.id)
				return true;
		}
		return false;
	}

	public boolean matches(ExchangeItem exchangeItem) {
		if (exchangeItem == null
			|| this.exchange == null
			|| exchangeItem.exchange == null)
			return false;
		if (!Objects.equals(exchange.flow, exchangeItem.exchange.flow))
			return false;
		return exchange.isInput != exchangeItem.exchange.isInput;
	}

	public boolean canBeReferenceFlow() {
		if (exchange == null || exchange.flow == null)
			return false;
		var flowType = exchange.flow.flowType;
		return getIOPane().isForInputs() == (flowType == FlowType.WASTE_FLOW)
			|| !getIOPane().isForInputs() == (flowType == FlowType.PRODUCT_FLOW);
	}

	public String toString() {
		var name = Labels.name(exchange.flow);
		return "ExchangeItem("
			+ name.substring(0, Math.min(name.length(), 20)) + ")";
	}

}
