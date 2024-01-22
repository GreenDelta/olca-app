package org.openlca.app.editors.graphical.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.swt.SWT;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.tools.graphics.model.Component;
import org.openlca.app.tools.graphics.model.Link;
import org.openlca.app.tools.graphics.model.Side;
import org.openlca.app.tools.graphics.themes.Theme;
import org.openlca.app.util.Labels;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.*;
import org.openlca.core.model.Process;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.model.descriptors.ProductSystemDescriptor;
import org.openlca.core.model.descriptors.ResultDescriptor;
import org.openlca.core.model.descriptors.RootDescriptor;

import static org.openlca.app.tools.graphics.layouts.GraphLayout.DEFAULT_LOCATION;

/**
 * A {@link Node} represents a unit process, a library process, a result
 * or a product system with its list of input or output flows (see
 * {@link IOPane}).
 */
public class Node extends MinMaxComponent {

	public static final String
			EXPANDED_PROP = "expanded",
			INPUT_PROP = "input",
			OUTPUT_PROP = "output";

	public static final Dimension DEFAULT_SIZE =
			new Dimension(250, SWT.DEFAULT);

	private final IDatabase db = Database.get();
	public RootDescriptor descriptor;
	private RootEntity entity;

	/**
	 * Define if the input or this output side is expanded.
	 * 0: not expanded, 1: input expanded, 2: output expanded, 3: both
	 * expanded
	 */
	public int isExpanded;
	/**
	 * Helper variable when exploring graph in CollapseCommand
	 */
	public boolean isCollapsing;
	private final String comparisonLabel = Labels.name(getRefFlow());
	private final Map<Side, Boolean> buttonStatus = new EnumMap<>(Side.class);

	public Node(RootDescriptor descriptor) {
		this.descriptor = descriptor;
		setLocation(DEFAULT_LOCATION);
		setSize(DEFAULT_SIZE);
	}

	/**
	 * The RootEntity of this is lazily instantiate in the constructor for
	 * computational reasons.
	 */
	public RootEntity getEntity() {
		// If the corresponding entity is dirty, the dirty one is return.
		if (entity == null
				&& getGraph() != null && getGraph().getEditor() != null) {
			entity = getGraph().getEditor().getDirty(descriptor.id);
		}
		// Otherwise, it is retrieved from the DB.
		if (entity == null) {
			entity = db.get(descriptor.type.getModelClass(), descriptor.id);
		}
		return entity;
	}

	public void setEntity(RootEntity entity) {
		this.entity = entity;
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
			if (type == FlowType.WASTE_FLOW && exchangeItem.matchesLink(link))
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
			if (type == FlowType.PRODUCT_FLOW && exchangeItem.matchesLink(link))
				return exchangeItem;
			if (type == FlowType.WASTE_FLOW)
				return exchangeItem;
		}
		return null;
	}

	private List<Node> getInputs() {
		return getAllTargetConnections().stream()
				.map(Link::getSourceNode)
				.filter(l -> l instanceof Node)
				.map(Node.class::cast)
				.toList();
	}

	private List<Node> getOutputs() {
		return getAllSourceConnections().stream()
				.map(Link::getTargetNode)
				.filter(l -> l instanceof Node)
				.map(Node.class::cast)
				.toList();
	}

	public static boolean isInput(FlowType type, boolean isProvider) {
		if (isProvider && type == FlowType.WASTE_FLOW)
			return true; // waste input
		return !isProvider && type == FlowType.PRODUCT_FLOW; // product input
	}

	public static boolean isOutput(FlowType type, boolean isProvider) {
		if (isProvider && type == FlowType.PRODUCT_FLOW)
			return true; // product output
		return !isProvider && type == FlowType.WASTE_FLOW; // waste output
	}

	@Override
	protected Dimension getMinimizedSize() {
		return new Dimension(getSize().width, DEFAULT_SIZE.height);
	}

	@Override
	protected Dimension getMaximizedSize() {
		return new Dimension(getSize().width, DEFAULT_SIZE.height);
	}

	@Override
	public void addChildren() {
		if (descriptor == null || descriptor.type == null)
			return;
		var editor = getGraph().getEditor();
		var panes = editor.getGraphFactory().createIOPanes(this);
		addChild(panes.get(INPUT_PROP), 0);
		addChild(panes.get(OUTPUT_PROP), 1);
	}

	public HashMap<String, IOPane> getIOPanes() {
		HashMap<String, IOPane> map = new HashMap<>();
		for (var child : getChildren())
			if (child instanceof IOPane pane)
				map.put(pane.isForInputs() ? INPUT_PROP : OUTPUT_PROP, pane);
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
			list.addAll(ioPane.getExchangeItems());
		}
		return list;
	}

	public boolean isEditable() {
		if (entity instanceof Process process) {
			return !process.isFromLibrary()
					&& process.processType == ProcessType.UNIT_PROCESS
					&& getGraph().getConfig().isNodeEditingEnabled();
		}
		return false;
	}

	public boolean isExpanded(Side side) {
		var bitPosition = side == Side.INPUT ? 1 : 2;
		return ((isExpanded >> bitPosition) & 1) == 1;
	}

	public void setExpanded(Side side, boolean value) {
		var oldIsExpanded = isExpanded;
		var bitPosition = side == Side.INPUT ? 1 : 2;
		if (value)
			isExpanded |= 1 << bitPosition;
		else
			isExpanded &= ~(1 << bitPosition);
		if (oldIsExpanded != isExpanded)
			firePropertyChange(EXPANDED_PROP, oldIsExpanded, isExpanded);
	}

	/**
	 * isExpanded can be set manually with a quick bit operation when expanding
	 * or collapsing the node. However, it is sometime necessary that the Node
	 * updates itself its status when it is not known out of the box.
	 */
	public void updateIsExpanded(Side side) {
		var sourceNodeIds = getAllTargetConnections().stream()
				.map(GraphLink.class::cast)
				.map(c -> c.getSourceNode().descriptor.id)
				.toList();
		var targetNodeIds = getAllSourceConnections().stream()
				.map(GraphLink.class::cast)
				.map(c -> c.getTargetNode().descriptor.id)
				.toList();

		for (var pLink : getGraph().linkSearch.getLinks(descriptor.id)) {
			FlowType type = getGraph().flows.type(pLink.flowId);
			if (type == null || type == FlowType.ELEMENTARY_FLOW)
				continue;

			boolean isProvider = descriptor.id == pLink.providerId;
			long otherID = isProvider ? pLink.processId : pLink.providerId;

			if (side == Side.INPUT && isInput(type, isProvider)) {
				if (!sourceNodeIds.contains(otherID)) {
					setExpanded(side, false);
					return;
				}
			} else if (side == Side.OUTPUT && isOutput(type, isProvider)) {
				if (!targetNodeIds.contains(otherID)) {
					setExpanded(side, false);
					return;
				}
			} else if (this.descriptor.id == otherID) {  // close loop
				if (!sourceNodeIds.contains(this.descriptor.id)
						|| !targetNodeIds.contains(this.descriptor.id)) {
					setExpanded(side, false);
					return;
				}
			}
		}
		setExpanded(side, true);
	}

	/**
	 * Return true if the process has at least one linked input.
	 */
	public boolean hasLinkedInput() {
		var linkSearch = getGraph().linkSearch;
		long processId = descriptor.id;

		for (ProcessLink link : linkSearch.getLinks(processId)) {
			FlowType type = getGraph().flows.type(link.flowId);
			boolean isProvider = link.providerId == processId;

			if (type == FlowType.PRODUCT_FLOW && !isProvider)
				return true;
			if (type == FlowType.WASTE_FLOW && isProvider)
				return true;
		}
		return false;
	}

	/**
	 * Return true if the process has at least one linked output.
	 */
	public boolean hasLinkedOutput() {
		var linkSearch = getGraph().linkSearch;
		long processId = descriptor.id;

		for (ProcessLink link : linkSearch.getLinks(processId)) {
			FlowType type = getGraph().flows.type(link.flowId);
			boolean isProvider = link.providerId == processId;

			if (type == FlowType.PRODUCT_FLOW && isProvider)
				return true;
			if (type == FlowType.WASTE_FLOW && !isProvider)
				return true;
		}
		return false;
	}

	public void setButtonStatus() {
		for (var side : Side.values()) {
			if (side == Side.INPUT ? !hasLinkedInput() : !hasLinkedOutput())
				buttonStatus.put(side, false);
			else if (!isExpanded(side))
				buttonStatus.put(side, true);
			else if (this.equals(getGraph().getReferenceNode()))
				buttonStatus.put(side, true);
			else {
				buttonStatus.put(side, hasConnectionToCollapse(side));
			}
		}
	}

	public boolean isButtonEnabled(Side side) {
		if (buttonStatus.get(side) == null)
			setButtonStatus();
		return buttonStatus.get(side);
	}

	/**
	 * Check if this Node has any linked providers (INPUT side) or suppliers
	 * (OUTPUT side) to collapse.
	 * Return true if the Node is the reference and has any supplier (resp.
	 * provider).
	 * The method checks if any of the suppliers (resp. providers) of the Node is
	 * not chaining to the reference Node. In other words, if it exists a supplier
	 * (resp. provider) such that there is no path connecting it to the reference
	 * Node (without traversing this Node), then this Node can be collapsed on
	 * this Side.
	 */
	public boolean hasConnectionToCollapse(Side side) {
		if (side == Side.BOTH)
			return false;

		var nodes = side == Side.INPUT ? getInputs() : getOutputs();

		if (getGraph().isReferenceProcess(this))
			return !nodes.isEmpty();

		var handled = new HashSet<Node>();
		handled.add(this);

		for (var node : nodes) {
			var isChainingReferenceNode = false;
			var queue = new ArrayDeque<Node>();
			queue.add(node);

			while (!queue.isEmpty()) {
				var next = queue.poll();

				if (getGraph().isReferenceProcess(next)) {
					isChainingReferenceNode = true;
					queue.clear();
					continue;
				}

				handled.add(next);

				var otherNodes = new ArrayList<Node>();
				otherNodes.addAll(next.getInputs());
				otherNodes.addAll(next.getOutputs());
				for (var otherNode : otherNodes) {
					if (!handled.contains(otherNode) && !queue.contains(otherNode)) {
						queue.add(otherNode);
					}
				}
			}
			if (!isChainingReferenceNode) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if any of this node's outputs or inputs chain to the reference node.
	 * Returns false is the initial node is the reference.
	 */
	public boolean isChainingReferenceNode(Side side) {
		if (side == Side.BOTH)
			return false;

		if (getGraph().isReferenceProcess(this))
			return false;

		var handled = new HashSet<Node>();
		handled.add(this);

		var nodes = side == Side.INPUT ? getInputs() : getOutputs();
		for (var node : nodes) {
			if (getGraph().isReferenceProcess(node))
				return true;

			var queue = new ArrayDeque<Node>();
			queue.add(node);

			while (!queue.isEmpty()) {
				var next = queue.poll();
				handled.add(next);

				var outputs = next.getOutputs();
				if (outputs == null)
					continue;
				for (var output : outputs) {
					if (getGraph().isReferenceProcess(output))
						return true;
					if (!handled.contains(output) && !queue.contains(output)) {
						queue.add(output);
					}
				}

				var inputs = next.getInputs();
				if (inputs == null)
					continue;
				for (var input : inputs) {
					if (getGraph().isReferenceProcess(input))
						return true;
					if (!handled.contains(input) && !queue.contains(input)) {
						queue.add(input);
					}
				}
			}
		}
		return false;
	}

	public void setDescriptor(RootDescriptor d) {
		if (d == null)
			return;
		descriptor = d;
	}

	public ExchangeItem getRefExchangeItem() {
		for (var item : getExchangeItems())
			if (item.isQuantitativeReference())
				return item;
		return null;
	}

	public ExchangeItem getExchangeItem(Exchange exchange) {
		for (var child : getChildren()) {
			if (child instanceof ExchangeItem item) {
				if (Objects.equals(item.exchange, exchange))
					return item;
			}
		}
		return null;
	}

	@Override
	public int compareTo(Component other) {
		if (other instanceof Node node) {
			var thisRefFlow = getRefFlow();
			var otherRefFlow = node.getRefFlow();
			if (thisRefFlow == null && otherRefFlow == null)
				return 0;
			if (thisRefFlow != null && otherRefFlow == null)
				return 1;
			if (thisRefFlow == null)
				return -1;
			return this.getComparisonLabel().compareTo(node.getComparisonLabel());
		} else return 0;
	}

	@Override
	public String getComparisonLabel() {
		return comparisonLabel;
	}

	Flow getRefFlow() {
		if (descriptor instanceof ProcessDescriptor) {
			var process = (Process) getEntity();
			if (process.quantitativeReference != null)
				return process.quantitativeReference.flow;
		} else if (descriptor instanceof ResultDescriptor) {
			var result = (Result) getEntity();
			if (result.referenceFlow != null)
				return result.referenceFlow.flow;
		} else if (descriptor instanceof ProductSystemDescriptor) {
			var productSystem = (ProductSystem) getEntity();
			if (productSystem.referenceExchange != null)
				return productSystem.referenceExchange.flow;
		}
		return null;
	}

	public boolean isOfReferenceProcess() {
		return getGraph().isReferenceProcess(this);
	}

	public Theme.Box getThemeBox() {
		return Theme.Box.of(descriptor, isOfReferenceProcess());
	}

	public String toString() {
		var editable = isEditable() ? "E-" : "";
		var prefix = isMinimized() ? M.Minimize : M.Maximize;
		var name = Labels.name(descriptor);
		return editable + "Node[" + prefix + "]("
				+ name.substring(0, Math.min(name.length(), 20)) + ")";
	}

	@Override
	public final int hashCode() {
		return descriptor == null ? super.hashCode() : descriptor.hashCode();
	}

}
