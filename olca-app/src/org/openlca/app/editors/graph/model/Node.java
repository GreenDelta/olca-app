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

	public static final Dimension DEFAULT_MINIMIZED_SIZE = new Dimension(250, 25);
	public static final Dimension DEFAULT_MAXIMIZED_SIZE = new Dimension(250, -1);

	public final RootDescriptor descriptor;
	private final ProcessExpander inputProcessExpander;
	private final ProcessExpander outputProcessExpander;

	public Node(RootDescriptor descriptor, GraphEditor editor) {
		super(editor);
		this.descriptor = descriptor;
		setSize(isMinimized() ? DEFAULT_MINIMIZED_SIZE : DEFAULT_MAXIMIZED_SIZE);
		inputProcessExpander = new ProcessExpander(Side.INPUT);
		outputProcessExpander = new ProcessExpander(Side.OUTPUT);
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

	public Graph getGraph() {
		return (Graph) getParent();
	}

	public boolean isEditable() {
		if (descriptor instanceof ProcessDescriptor p) {
			return !p.isFromLibrary() && p.processType == ProcessType.UNIT_PROCESS;
		}
		return false;
	}

	public ProcessExpander processExpanderOf(Side side) {
		return side.equals(Side.INPUT)
			? inputProcessExpander
			: outputProcessExpander;
	}

	public void collapse() {
		for (Side side: Side.values())
			collapse(side);
	}

	public void collapse(Side side) {
		if (!isExpanded(side))
			return;
		processExpanderOf(side).collapse(this);
	}

	/**
	 * Used to avoid removing the initial node while collapsing, should only be
	 * called from within ProcessExpander.collapse
	 */
	public void collapse(Side side, Node initialNode) {
		if (!isExpanded(side))
			return;
		processExpanderOf(side).collapse(initialNode);
	}

	public void expand() {
		for (Side side: Side.values())
			if (processExpanderOf(side) != null)
				expand(side);
	}

	public void setExpanded(Side side, boolean value) {
		if (side == null)
			return;
		processExpanderOf(side).setExpanded(value);
	}

	public void expand(Side side) {
		processExpanderOf(side).expand();
	}

	public boolean isExpanded(Side side) {
		if (processExpanderOf(side) == null)
			return false;
		else return processExpanderOf(side).isExpanded();
	}

	public boolean shouldProcessExpanderBeVisible(Side side) {
		if (processExpanderOf(side) == null)
			return false;
		else return processExpanderOf(side).shouldBeVisible();
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

	private class ProcessExpander {

		private final Side side;
		private boolean expanded;
		// isCollapsing is used to prevent endless recursion in collapse()
		private boolean isCollapsing;

		private ProcessExpander(Side side) {
			this.side = side;
		}

		public boolean isExpanded() {
			return expanded;
		}

		public void setExpanded(boolean value) {
			expanded = value;
		}

		public boolean shouldBeVisible() {
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

		static boolean canExpand(Node node, Side side) {
			var graph = node.getGraph();
			MutableProcessLinkSearchMap linkSearch = graph.linkSearch;
			long processId = node.descriptor.id;
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
			expanded = true;
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
			expanded = false;
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
					outNode = graph.getOrCreateProcessNode(otherID);
					graph.addChild(outNode);
				} else if (isOutputNode(type, isProvider)) {
					outNode = Node.this;
					inNode = graph.getOrCreateProcessNode(otherID);
					graph.addChild(inNode);
				} else {
					continue;
				}
				new Link(pLink, inNode, outNode);
			}
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
