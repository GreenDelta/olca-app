package org.openlca.app.editors.graphical_legacy.model;

import java.util.Objects;

import org.openlca.app.editors.graphical_legacy.GraphEditor;
import org.openlca.app.editors.graphical_legacy.view.ExchangeFigure;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.descriptors.ProcessDescriptor;

/**
 * A {@link ExchangeNode} represents an input or output of a flow.
 */
public class ExchangeNode extends Node {

	public final Exchange exchange;

	ExchangeNode(GraphEditor editor, Exchange exchange) {
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
		var parent = parent();
		if (parent == null)
			return false;
		if (parent.process instanceof ProcessDescriptor p) {
			return p.quantitativeReference != null
				&& p.quantitativeReference == exchange.id;
		}
		// TODO: product systems and results ...

		return false;
	}

	public boolean matches(ExchangeNode node) {
		if (node == null || this.exchange == null || node.exchange == null)
			return false;
		if (!Objects.equals(exchange.flow, node.exchange.flow))
			return false;
		return exchange.isInput != node.exchange.isInput;
	}

	/**
	 * @deprecated this method should go and called from the respective EditPart
	 */
	@Deprecated
	public void setHighlighted(boolean value) {
		((ExchangeFigure) figure).setHighlighted(value);
	}

	@Override
	public ProcessNode parent() {
		return (ProcessNode) super.parent().parent();
	}

	/**
	 * Returns true if this exchange is connected.
	 */
	public boolean isConnected() {
		var parent = parent();
		if (parent == null || parent.process == null || exchange == null)
			return false;
		var linkSearch = parent.parent().linkSearch;
		var links = linkSearch.getConnectionLinks(parent.process.id);
		for (ProcessLink link : links) {
			if (link.exchangeId == exchange.id)
				return true;
		}
		return false;
	}

}
