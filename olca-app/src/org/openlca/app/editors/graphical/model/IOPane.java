package org.openlca.app.editors.graphical.model;

import org.openlca.app.tools.graphics.model.Component;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowType;

import java.util.List;


/**
 * An {@link IOPane} represents a list of exchanges to be displayed. Each
 * exchange is represented by a {@link ExchangeItem}.
 */
public class IOPane extends Component {

	private final boolean forInputs;

	public IOPane(boolean forInputs) {
		this.forInputs = forInputs;
	}

	public boolean isForInputs() {
		return forInputs;
	}

	public boolean accepts(Flow flow) {
		if ((forInputs && flow.flowType == FlowType.WASTE_FLOW)
				|| (!forInputs && flow.flowType == FlowType.PRODUCT_FLOW)) {
			return getExchangeItems().stream()
					.noneMatch(item -> item.exchange.flow.id == flow.id);
		} else
			return true;
	}

	public boolean hasOnlyElementary() {
		for (var item : getExchangeItems())
			if (!item.isElementary())
				return false;
		return true;
	}

	@SuppressWarnings("unchecked")
	public List<ExchangeItem> getExchangeItems() {
		return (List<ExchangeItem>) super.getChildren();
	}

	public Node getNode() {
		return (Node) getParent();
	}

	public Graph getGraph() {
		return getNode().getGraph();
	}

	public String toString() {
		var label = forInputs ? "input" : "output";
		return "IOPane(" + label + ")";
	}

	@Override
	public int compareTo(Component other) {
		if (other == null)
			return 1;
		if (other instanceof IOPane pane) {
			if (this.forInputs == pane.forInputs) return 0;
			return this.forInputs ? 1 : -1;
		}
		else return 0;
	}

}
