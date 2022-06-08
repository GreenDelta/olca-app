package org.openlca.app.editors.graphical.model;

import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.util.Labels;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.descriptors.ProcessDescriptor;

import java.util.Objects;

public class ExchangeItem extends GraphComponent {

	public final Exchange exchange;

	public ExchangeItem(GraphEditor editor, Exchange exchange) {
		super(editor);
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
		if (parent.descriptor instanceof ProcessDescriptor p) {
			return p.quantitativeReference != null
				&& p.quantitativeReference == exchange.id;
		}
		// TODO: product systems and results ...

		return false;
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

	public String toString() {
		var name = Labels.name(exchange.flow);
		return "ExchangeItem("
			+ name.substring(0, Math.min(name.length(), 20)) + ")";
	}

}
