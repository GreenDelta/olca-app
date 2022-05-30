package org.openlca.app.editors.graph.model;

import org.eclipse.draw2d.geometry.Dimension;
import org.openlca.app.M;
import org.openlca.app.editors.graph.GraphEditor;
import org.openlca.app.editors.graph.search.MutableProcessLinkSearchMap;
import org.openlca.app.util.Labels;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.model.descriptors.RootDescriptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A {@link Node} represents a unit process, a library process, a result
 * or a product system with its list of input or output flows (see
 * {@link IOPane}).
 */
public class Node extends MinMaxGraphComponent {

	public static final String EXPANSION_PROP = "expansion";

	public static final Dimension DEFAULT_MINIMIZED_SIZE = new Dimension(250, 25);
	public static final Dimension DEFAULT_MAXIMIZED_SIZE = new Dimension(250, -1);

	public final RootDescriptor descriptor;
	private final Expander inputExpander = new Expander(Side.INPUT);
	private final Expander outputExpander = new Expander(Side.OUTPUT);

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
		addChild(panes.get(INPUT_PROP), 0);
		addChild(panes.get(OUTPUT_PROP), 1);
	}

	public Graph getGraph() {
		return (Graph) getParent();
	}

	@SuppressWarnings("unchecked")
	public HashMap<String, IOPane> getIOPanes() {
		HashMap<String, IOPane> map = new HashMap<>();
		for (IOPane pane : (List<IOPane>) super.getChildren()) {
			map.put(pane.isForInputs() ? INPUT_PROP : OUTPUT_PROP, pane);
		}
		return map;
	}

	public IOPane getInputIOPane() {
		return getIOPanes().get(INPUT_PROP);
	}

	public IOPane getOutputIOPane() {
		return getIOPanes().get(OUTPUT_PROP);
	}

	public List<ExchangeItem> getExchangeItems() {
		List<ExchangeItem> list = new ArrayList<>();
		for (IOPane ioPane : getIOPanes().values()) {
			list.addAll(ioPane.getExchangesItems());
		}
		return list;
	}

	public boolean isEditable() {
		if (descriptor instanceof ProcessDescriptor p) {
			return !p.isFromLibrary() && p.processType == ProcessType.UNIT_PROCESS;
		}
		return false;
	}

	public Expander expanderOf(Side side) {
		return side.equals(Side.INPUT)
			? inputExpander
			: outputExpander;
	}

	public void collapse() {
		for (Side side: Side.values())
			collapse(side);
	}

	public void collapse(Side side) {
		if (!isExpanded(side))
			return;
		expanderOf(side).collapse(this);
	}

	/**
	 * Used to avoid removing the initial node while collapsing, should only be
	 * called from within Expander.collapse
	 */
	public void collapse(Side side, Node initialNode) {
		if (!isExpanded(side))
			return;
		expanderOf(side).collapse(initialNode);
	}

	public void expand() {
		for (Side side: Side.values())
			if (expanderOf(side) != null)
				expand(side);
	}

	public void setExpanded(Side side, boolean value) {
		if (side == null)
			return;
		expanderOf(side).setExpanded(value);
	}

	public void expand(Side side) {
		expanderOf(side).expand();
	}

	public boolean isExpanded(Side side) {
		if (expanderOf(side) == null)
			return false;
		else return expanderOf(side).isExpanded();
	}

	public boolean shouldExpanderBeVisible(Side side) {
		if (expanderOf(side) == null)
			return false;
		else return expanderOf(side).canExpand();
	}


	public String toString() {
		var prefix = isMinimized() ? M.Minimize : M.Maximize;
		var name = Labels.name(descriptor);
		return "Node[" + prefix + "]("
			+ name.substring(0, Math.min(name.length(), 20)) + ")";
	}

	public enum Side {
		INPUT, OUTPUT
	}

	private class Expander {

		private final Side side;
		private boolean expanded;
		// isCollapsing is used to prevent endless recursion in collapse()
		private boolean isCollapsing;

		private Expander(Side side) {
			this.side = side;
		}

		public boolean isExpanded() {
			return expanded;
		}

		public void setExpanded(boolean value) {
			if (value != expanded) {
				expanded = value;
				firePropertyChange(EXPANSION_PROP, null, value);
			}
		}

		public boolean canExpand() {
			var graph = getGraph();
			MutableProcessLinkSearchMap linkSearch = graph.linkSearch;
			long processId = descriptor.id;
			for (ProcessLink link : linkSearch.getLinks(processId)) {
				FlowType type = graph.flows.type(link.flowId);
				boolean isProvider = link.providerId == processId;
				if (side == Side.INPUT) {
					if (type == FlowType.PRODUCT_FLOW && !isProvider)
						return true;
					if (type == FlowType.WASTE_FLOW && isProvider)
						return true;
				} else if (side == Side.OUTPUT) {
					if (type == FlowType.PRODUCT_FLOW && isProvider)
						return true;
					if (type == FlowType.WASTE_FLOW && !isProvider)
						return true;
				}
			}
			return false;
		}

		private void expand() {
			createNecessaryNodes();
			setExpanded(true);
		}

		private void collapse(Node initialNode) {
			if (isCollapsing)
				return;
			isCollapsing = true;
			// need to copy the links otherwise we get a
			// concurrent modification exception
			var links = getAllLinks().toArray(new Link[0]);
			for (var link : links) {
				var thisNode = side == Side.INPUT
					? link.getTargetNode()
					: link.getSourceNode();
				var otherNode = side == Side.INPUT
					? link.getSourceNode()
					: link.getTargetNode();
				if (!thisNode.equals(Node.this))
					continue;
				link.disconnect();
				otherNode.collapse(Side.INPUT, initialNode);
				otherNode.collapse(Side.OUTPUT, initialNode);
				if (otherNode.equals(initialNode))
					continue;
				if (!otherNode.getAllLinks().isEmpty())
					continue;
				getGraph().removeChild(otherNode);
			}
			isCollapsing = false;
			setExpanded(false);
		}

		private void createNecessaryNodes() {
			var graph = getGraph();
			long processID = descriptor.id;
			List<ProcessLink> links = graph.linkSearch.getLinks(processID);
			for (ProcessLink pLink : links) {
				FlowType type = graph.flows.type(pLink.flowId);
				if (type == null || type == FlowType.ELEMENTARY_FLOW)
					continue;
				boolean isProvider = processID == pLink.providerId;
				long otherID = isProvider ? pLink.processId : pLink.providerId;
				Node outNode;
				Node inNode;
				if (isInputNode(type, isProvider)) {
					inNode = Node.this;
					outNode = createNode(otherID);
				} else if (isOutputNode(type, isProvider)) {
					outNode = Node.this;
					inNode = createNode(otherID);
				} else {
					continue;
				}
				new Link(pLink, outNode, inNode);
			}
		}

		/**
		 * Create, if necessary, a node using the <code>GraphFactory</code>.
		 * @return Return the existent or the newly created <code>Node</code> for
		 * convenience.
		 */
		private Node createNode(long id) {
			var graph = getGraph();

			// Checking if the node already exists.
			var node = graph.getProcessNode(id);
			if (node != null)
				return node;

			var descriptor = GraphFactory.getDescriptor(id);
			node = editor.getGraphFactory().createNode(descriptor, null);
			graph.addChild(node);
			return node;
		}

		private boolean isInputNode(FlowType type, boolean isProvider) {
			if (side != Side.INPUT)
				return false;
			if (isProvider && type == FlowType.WASTE_FLOW)
				return true; // waste input
			return !isProvider && type == FlowType.PRODUCT_FLOW; // product input
		}

		private boolean isOutputNode(FlowType type, boolean isProvider) {
			if (side != Side.OUTPUT)
				return false;
			if (isProvider && type == FlowType.PRODUCT_FLOW)
				return true; // product output
			return !isProvider && type == FlowType.WASTE_FLOW; // waste output
		}

	}

}
