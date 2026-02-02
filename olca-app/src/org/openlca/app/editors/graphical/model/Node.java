package org.openlca.app.editors.graphical.model;

import static org.openlca.app.components.graphics.layouts.GraphLayout.*;

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
import org.openlca.app.components.graphics.model.Component;
import org.openlca.app.components.graphics.model.Link;
import org.openlca.app.components.graphics.model.Side;
import org.openlca.app.components.graphics.themes.Theme;
import org.openlca.app.db.Database;
import org.openlca.app.util.Labels;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.Result;
import org.openlca.core.model.RootEntity;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.model.descriptors.ProductSystemDescriptor;
import org.openlca.core.model.descriptors.ResultDescriptor;
import org.openlca.core.model.descriptors.RootDescriptor;

/**
 * A {@link Node} represents a unit process, a library process, a result
 * or a product system with its list of input or output flows (see
 * {@link IOPane}).
 */
public class Node extends MinMaxComponent {

	public static final String
			EXPANDED_PROP = "expanded",
			INPUT_PROP = "input",
			OUTPUT_PROP = "output",
			GROUP_PROP = "group";

	public static final Dimension DEFAULT_SIZE =
			new Dimension(250, SWT.DEFAULT);

	private final IDatabase db = Database.get();
	public final RootDescriptor descriptor;
	private RootEntity entity;

	/// Defines the expansion state of the input and output side:
	/// + 0: not expanded
	/// + 1: input side expanded
	/// + 2: output side expanded
	/// + 3: both sides expanded
	private int expansionState;

	private String comparisonLabel;
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
		if (side == null)
			return false;
		return switch (side) {
			case INPUT -> (expansionState & 1) == 1;
			case OUTPUT -> (expansionState & 2) == 2;
			case BOTH -> (expansionState & 3) == 3;
		};
	}

	public void setExpanded(Side side, boolean value) {
		if (side == null)
			return;
		int oldState = expansionState;
		expansionState = switch (side) {
			case INPUT -> value ? expansionState | 1 : expansionState & ~1;
			case OUTPUT -> value ? expansionState | 2 : expansionState & ~2;
			case BOTH -> value ? 3 : 0;
		};
		if (oldState != expansionState) {
			notifyChange(EXPANDED_PROP, oldState, expansionState);
		}
	}

	/**
	 * isExpanded can be set manually with a quick bit operation when expanding
	 * or collapsing the node. However, it is sometime necessary that the Node
	 * updates itself its status when it is not known out of the box.
	 */
	public void updateIsExpanded(Side side) {
		if (side == Side.BOTH) {
			updateIsExpanded(Side.INPUT);
			updateIsExpanded(Side.OUTPUT);
		}

		var sourceNodeIds = getAllTargetConnections().stream()
				.map(GraphLink.class::cast)
				.map(c -> c.getSourceNode().descriptor.id)
				.toList();
		var targetNodeIds = getAllSourceConnections().stream()
				.map(GraphLink.class::cast)
				.map(c -> c.getTargetNode().descriptor.id)
				.toList();

		for (var pLink : getGraph().linkSearch.getAllLinks(descriptor.id)) {
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

		for (ProcessLink link : linkSearch.getAllLinks(processId)) {
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

		for (ProcessLink link : linkSearch.getAllLinks(processId)) {
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
		setButtonStatus(Side.INPUT);
		setButtonStatus(Side.OUTPUT);
	}

	/**
	 * Set the button status of the corresponding side.
	 * It does nothing when side is Side.BOTH.
	 */
	private void setButtonStatus(Side side) {
		if (side == Side.BOTH) {
			return;
		}

		if (side == Side.INPUT ? !hasLinkedInput() : !hasLinkedOutput()) {
			buttonStatus.put(side, false);
		}
		else if (!isExpanded(side)) {
			buttonStatus.put(side, true);
		}
		else if (this.equals(getGraph().getReferenceNode())) {
			buttonStatus.put(side, true);
		}
		else {
			buttonStatus.put(side, hasConnectionToCollapse(side));
		}
	}

	/**
	 * Returns the status for the corresponding button.
	 * Returns false if the side is Side.BOTH.
	 */
	public boolean isButtonEnabled(Side side) {
		if (side == Side.BOTH) {
			return false;
		}

		if (buttonStatus.get(side) == null)
			setButtonStatus();
		return buttonStatus.get(side);
	}

	/**
	 * Check if this Node has any linked providers (INPUT side) or suppliers
	 * (OUTPUT side) to collapse.
	 * Return true if the Node is the reference and has any supplier (resp.
	 * provider).
	 * The method checks if any of the suppliers (resp. providers) of the Node are
	 * not chained to the reference Node. In other words, if it exists a supplier
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
				for (var output : outputs) {
					if (getGraph().isReferenceProcess(output))
						return true;
					if (!handled.contains(output) && !queue.contains(output)) {
						queue.add(output);
					}
				}

				var inputs = next.getInputs();
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
		if (comparisonLabel == null) {
			comparisonLabel = Labels.name(getRefFlow());
		}
		return comparisonLabel;
	}

	protected Flow getRefFlow() {
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

	public boolean isReferenceProcess() {
		return getGraph().isReferenceProcess(this);
	}

	public Theme.Box getThemeBox() {
		return Theme.Box.of(descriptor, isReferenceProcess());
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

	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (obj == this) return true;
		if (!(obj instanceof Node other)) return false;
		return Objects.equals(this.descriptor, other.descriptor);
	}
}
