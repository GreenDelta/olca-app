package org.openlca.app.editors.graphical.model;

import org.openlca.app.tools.graphics.model.Component;
import org.openlca.app.util.Labels;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ProcessLink;
import org.openlca.util.Strings;

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

	/**
	 * Check if this is the quantitative reference.
	 */
	public boolean isQuantitativeReference() {
		return isRefFlow()
				&& ((isWaste() && getIOPane().isForInputs())
				|| (!isWaste() && !getIOPane().isForInputs()));
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
	 * Note that link.exchangeId is compared to exchange.internalId when the
	 * process is not saved.
	 */
	public boolean isConnected() {
		var node = getNode();
		if (node == null || node.descriptor == null || exchange == null)
			return false;
		var linkSearch = node.getGraph().linkSearch;
		var links = linkSearch.getConnectionLinks(node.descriptor.id);
		for (ProcessLink link : links) {
			if (matchesLink(link))
				return true;
		}
		return false;
	}

	/**
	 * Check if the two exchange items can be linked.
	 * This method does not check if one of the item is already connected.
	 */
	public boolean matches(ExchangeItem other) {
		if (other == null	|| this.exchange == null || other.exchange == null)
			return false;
		if (!Objects.equals(exchange.flow, other.exchange.flow))
			return false;
		return exchange.isInput != other.exchange.isInput;
	}

	/**
	 * Check if this <code>ExchangeItem.exchange</code> matches the
	 * <code>ProcessLink</code> exchange in a saved or unsaved process context.
	 */
	public boolean matchesLink(ProcessLink link) {
		if (getGraph() != null && getGraph().getEditor() != null
				&& getGraph().getEditor().isDirty(getNode().getEntity())) {
			return link.exchangeId == exchange.internalId;
		} else
			return link.exchangeId == exchange.id;
	}

	public boolean canBeReferenceFlow() {
		if (exchange == null || exchange.flow == null)
			return false;
		var flowType = exchange.flow.flowType;
		return getIOPane().isForInputs() == (flowType == FlowType.WASTE_FLOW)
			|| !getIOPane().isForInputs() == (flowType == FlowType.PRODUCT_FLOW);
	}

	public boolean isElementary() {
		return exchange.flow.flowType == FlowType.ELEMENTARY_FLOW;
	}

	private static int typeOrderOf(Exchange e) {
		if (e == null
				|| e.flow == null
				|| e.flow.flowType == null)
			return -1;
		return switch (e.flow.flowType) {
			case PRODUCT_FLOW -> 0;
			case WASTE_FLOW -> 1;
			default -> 2;
		};
	}

	public static int compare(ExchangeItem e1, ExchangeItem e2) {
		if (e1 == null && e2 == null)
			return 0;
		if (e1 != null && e2 == null)
			return 1;
		if (e1 == null)
			return -1;
		int t1 = typeOrderOf(e1.exchange);
		int t2 = typeOrderOf(e2.exchange);
		if (t1 != t2)
			return t1 - t2;
		var name1 = Labels.name(e1.exchange.flow);
		var name2 = Labels.name(e2.exchange.flow);
		return Strings.compare(name1, name2);
	}

	@Override
	public int compareTo(Component other) {
		if (other instanceof ExchangeItem e)
			return compare(this, e);
		else return 0;
	}

	public String toString() {
		var name = Labels.name(exchange.flow);
		return "ExchangeItem("
				+ name.substring(0, Math.min(name.length(), 20)) + ")";
	}

}
