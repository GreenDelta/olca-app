package org.openlca.app.editors.graph.model;

import org.eclipse.draw2d.geometry.Dimension;
import org.openlca.app.M;
import org.openlca.app.editors.graph.GraphEditor;
import org.openlca.app.util.Labels;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.descriptors.RootDescriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link Node} represents a unit process, a library process, a result
 * or a product system with its list of input or output flows (see
 * {@link IOPane}).
 */
public class Node extends MinMaxGraphComponent {

	private static final Dimension DEFAULT_MINIMIZED_SIZE = new Dimension(250, 25);
	private static final Dimension DEFAULT_MAXIMIZED_SIZE = new Dimension(250, 300);

	public final RootDescriptor descriptor;

	public Node(RootDescriptor descriptor, GraphEditor editor) {
		super(editor);
		this.descriptor = descriptor;
		setSize(isMinimized() ? DEFAULT_MINIMIZED_SIZE : DEFAULT_MAXIMIZED_SIZE);
	}

	public ExchangeItem getOutput(ProcessLink link) {
		if (link == null)
			return null;
		FlowType type = getGraph().flows.type(link.flowId);
		if (type == null || type == FlowType.ELEMENTARY_FLOW)
			return null;
		for (ExchangeItem exchangeItem : getExchangeItems()) {
			Exchange exchange = exchangeItem.exchange;
			if (exchange == null || exchange.isInput ||
				exchange.flow == null || exchange.flow.id != link.flowId)
				continue;
			if (type == FlowType.PRODUCT_FLOW)
				return exchangeItem;
			if (type == FlowType.WASTE_FLOW
				&& exchange.id == link.exchangeId)
				return exchangeItem;
		}
		return null;
	}

	public ExchangeItem getInput(ProcessLink link) {
		if (link == null)
			return null;
		FlowType type = getGraph().flows.type(link.flowId);
		if (type == null || type == FlowType.ELEMENTARY_FLOW)
			return null;
		for (ExchangeItem exchangeItem : getExchangeItems()) {
			Exchange exchange = exchangeItem.exchange;
			if (exchange == null || !exchange.isInput ||
				exchange.flow == null || exchange.flow.id != link.flowId)
				continue;
			if (type == FlowType.PRODUCT_FLOW
				&& exchange.id == link.exchangeId)
				return exchangeItem;
			if (type == FlowType.WASTE_FLOW)
				return exchangeItem;
		}
		return null;
	}

	@Override
	protected Dimension getMinimizedSize() {
		return DEFAULT_MINIMIZED_SIZE;
	}

	@Override
	protected Dimension getMaximizedSize() {
		return DEFAULT_MAXIMIZED_SIZE;
	}

	@Override
	public void addChildren() {
		if (descriptor == null || descriptor.type == null)
			return;
		var panes = editor.getGraphFactory().createIOPanes(descriptor);
		addChild(panes.get("input"), 0);
		addChild(panes.get("output"), 1);
	}

	@SuppressWarnings("unchecked")
	public List<IOPane> getIOPanes() {
		return (List<IOPane>) super.getChildren();
	}

	public List<ExchangeItem> getExchangeItems() {
		List<ExchangeItem> list = new ArrayList<>();
		for (IOPane ioPane : getIOPanes()) {
			list.addAll(ioPane.getExchangesItems());
		}
		return list;
	}

	public Graph getGraph() {
		return (Graph) getParent();
	}

	public String toString() {
		var prefix = isMinimized() ? M.Minimize : M.Maximize;
		var name = Labels.name(descriptor);
		return "Node[" + prefix + "]("
			+ name.substring(0, Math.min(name.length(), 20)) + ")";
	}

}
