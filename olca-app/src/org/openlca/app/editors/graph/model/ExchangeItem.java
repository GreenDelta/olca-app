package org.openlca.app.editors.graph.model;

import org.openlca.app.editors.graph.GraphEditor;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.descriptors.ProcessDescriptor;

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

	public Node getNode() {
		return (Node) getParent().getParent();
	}

	public Graph getGraph() {return (Graph) getNode().getParent();}

	public Exchange getExchange() {
		return exchange;
	}

}
