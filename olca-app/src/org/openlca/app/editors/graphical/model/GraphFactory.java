package org.openlca.app.editors.graphical.model;

import java.util.*;

import com.google.gson.JsonArray;
import org.eclipse.draw2d.geometry.Point;
import org.openlca.app.db.Database;
import org.openlca.app.db.Libraries;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.GraphFile;
import org.openlca.app.editors.graphical.layouts.NodeLayoutInfo;
import org.openlca.app.editors.graphical.layouts.StickyNoteLayoutInfo;
import org.openlca.app.editors.graphical.model.commands.ExpandCommand;
import org.openlca.core.model.*;
import org.openlca.core.model.Process;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.core.model.descriptors.RootDescriptor;

import static org.openlca.app.editors.graphical.model.Node.INPUT_PROP;
import static org.openlca.app.editors.graphical.model.Node.OUTPUT_PROP;
import static org.openlca.app.tools.graphics.model.Side.INPUT;
import static org.openlca.app.tools.graphics.model.Side.OUTPUT;

/**
 * This class provides methods to initialize the model objects before drawing
 * any figures.
 * Note that after drawing, any changes made to the model should go through the
 * controller.
 */
public class GraphFactory {

	private final GraphEditor editor;

	public GraphFactory(GraphEditor editor) {
		this.editor = editor;
	}

	public Node createNode(RootDescriptor descriptor, NodeLayoutInfo info) {
		if (descriptor == null || descriptor.type == null)
			return null;

		var node = applyInfo(new Node(descriptor), info);

		// A Node (MinMaxGraphComponent) `minimized` attribute is by default true.
		if (!node.isMinimized()) {
			var panes = createIOPanes(node);
			node.addChild(panes.get(INPUT_PROP), 0);
			node.addChild(panes.get(OUTPUT_PROP), 1);
		}

		return node;
	}

	/**
	 * Mutate <code>node</code> with the <code>NodeLayoutInfo</code> provided.
	 *
	 * @return Mutated Node object.
	 */
	private Node applyInfo(Node node, NodeLayoutInfo info) {
		if (node == null || (info == null))
			return node;

		node.setMinimized(info.minimized);
		node.setSize(info.size);
		node.setLocation(info.location);

		node.setExpanded(INPUT, info.expandedLeft);
		node.setExpanded(OUTPUT, info.expandedRight);

		return node;
	}

	public HashMap<String, IOPane> createIOPanes(Node node) {
		var panes = new HashMap<String, IOPane>();
		panes.put(INPUT_PROP, new IOPane(true));
		panes.put(OUTPUT_PROP, new IOPane(false));

		var exchanges = getExchanges(node.getEntity(), node.descriptor.type);

		// filter and sort the exchanges
		exchanges.stream()
				.filter(e -> {
					if (e.flow == null)
						return false;
					return editor.config.showElementaryFlows()
							|| e.flow.flowType != FlowType.ELEMENTARY_FLOW;
				})
				.map(ExchangeItem::new)
				.sorted(ExchangeItem::compareTo)
				.forEach(e -> panes.get(paneOf(e)).addChild(e));
		return panes;
	}

	private String paneOf(ExchangeItem item) {
		if (item == null || item.exchange == null)
			return OUTPUT_PROP;
		var e = item.exchange;
		if (e.isAvoided)
			return e.isInput ? OUTPUT_PROP : INPUT_PROP;
		return e.isInput ? INPUT_PROP : OUTPUT_PROP;
	}

	/**
	 * Update an old ExchangeItem with a new one by removing the old one, and
	 * adding the new one.
	 */
	public static void updateExchangeItem(ExchangeItem oldValue,
			ExchangeItem newValue) {
		var ioPane = oldValue.getIOPane();

		var sourceLink = oldValue.getSourceConnections();
		var targetLink = oldValue.getTargetConnections();

		// Disconnecting the links before removing pane's child.
		for (var link : oldValue.getAllLinks())
			link.disconnect();

		var index = ioPane.getChildren().indexOf(oldValue);
		ioPane.removeChild(oldValue);
		ioPane.addChild(newValue, index);

		for (var link : sourceLink)
			link.reconnect(newValue, link.getTarget());
		for (var link : targetLink)
			link.reconnect(link.getSource(), newValue);
	}

	public static List<Exchange> getExchanges(RootEntity entity, ModelType type) {
		return switch (type) {
			case PROCESS -> {
				var p = (Process) entity;
				if (p == null)
					yield Collections.emptyList();
				if (p.isFromLibrary()) {
					Libraries.fillExchangesOf(p);
				}
				yield p.exchanges;
			}
			case PRODUCT_SYSTEM -> {
				var system = (ProductSystem) entity;
				yield system == null || system.referenceExchange == null
						? Collections.emptyList()
						: Collections.singletonList(system.referenceExchange);
			}
			case RESULT -> {
				var result = (Result) entity;
				var refFlow = result.referenceFlow;
				if (refFlow == null)
					yield Collections.emptyList();
				var e = new Exchange();
				e.isInput = refFlow.isInput;
				e.flow = refFlow.flow;
				e.amount = refFlow.amount;
				e.flowPropertyFactor = refFlow.flowPropertyFactor;
				e.unit = refFlow.unit;
				e.location = refFlow.location;
				yield Collections.singletonList(e);
			}
			default -> Collections.emptyList();
		};
	}

	public static List<Exchange> getConsumers(RootEntity entity, ModelType type) {
		return getExchanges(entity, type).stream()
				.filter(e -> (e.isInput && e.flow.flowType == FlowType.PRODUCT_FLOW)
						|| (!e.isInput && e.flow.flowType == FlowType.WASTE_FLOW))
				.toList();
	}

	public static void createGraphLink(Graph graph, ProcessLink pLink) {
		var provider = graph.getNode(pLink.providerId);
		var recipient = graph.getNode(pLink.processId);
		if (provider == null || recipient == null)
			return;
		var type = graph.flows.type(pLink.flowId);
		var source = type == FlowType.PRODUCT_FLOW ? provider
				: type == FlowType.WASTE_FLOW ? recipient
				: null;
		var target = type == FlowType.PRODUCT_FLOW ? recipient
				: type == FlowType.WASTE_FLOW ? provider
				: null;
		if (target == null)
			return;
		var link = new GraphLink(pLink, source, target);
		graph.mapProcessLinkToGraphLink.put(pLink, link);
	}

	public Graph createGraph(GraphEditor editor, JsonArray nodeArray,
			JsonArray stickyNoteArray) {
		if ((nodeArray == null) || (stickyNoteArray == null))
			return createGraph(editor);

		var graph = new Graph(editor);

		var system = editor.getProductSystem();
		var referenceProcess = system.referenceProcess;

		// Create the reference node.
		if (referenceProcess != null) {
			var refNodeInfo = getNodeInfo(nodeArray, referenceProcess.refId);
			var descriptor = getDescriptor(referenceProcess.id);
			var refNode = createNode(descriptor, refNodeInfo);
			if (refNode != null) {
				graph.addChild(refNode);
			}
		}

		// Create other nodes.
		for (var elem : nodeArray) {
			if (!elem.isJsonObject())
				continue;
			var obj = elem.getAsJsonObject();
			var info = GraphFile.toNodeLayoutInfo(obj);
			if (info == null)
				continue;

			// The reference should not be created again.
			if (referenceProcess != null
					&& Objects.equals(info.id, referenceProcess.refId))
				continue;

			var descriptor = getDescriptor(info.id);
			var node = createNode(descriptor, info);
			if (node == null)
				continue;
			graph.addChild(node);
		}

		var pLinks = graph.linkSearch.getLinks(graph.getChildrenIds());
		for (var pLink : pLinks) {
			createGraphLink(graph, pLink);
		}

		// Create the sticky notes
		for (var elem : stickyNoteArray) {
			if (!elem.isJsonObject())
				continue;
			var obj = elem.getAsJsonObject();
			var info = GraphFile.toStickyNoteLayoutInfo(obj);
			if (info == null)
				continue;
			var note = createStickyNote(info);
			if (note == null)
				continue;
			graph.addChild(note);
		}
		return graph;
	}

	private Graph createGraph(GraphEditor editor) {
		// No saved settings applied => try to find a good configuration
		var graph = new Graph(editor);

		var system = editor.getProductSystem();
		var referenceProcess = system.referenceProcess;

		if (referenceProcess != null) {
			var descriptor = getDescriptor(referenceProcess.id);
			var info = new NodeLayoutInfo((Point) null, null, false, false, false);
			var refNode = createNode(descriptor, info);
			if (refNode != null) {
				graph.addChild(refNode);
				for (var side : Arrays.asList(INPUT, OUTPUT)) {
					var command = new ExpandCommand(refNode, side, true);
					if (command.canExecute())
						command.execute();
				}
			}
		}

		// Set the graph settings to edit mode if the reference node has no output.
		var outputPane = graph.getReferenceNode().getOutputIOPane();
		if (outputPane.getChildren().isEmpty()) {
			var config = editor.config.copy();
			config.setNodeEditingEnabled(true);
			config.setShowElementaryFlows(true);
			config.copyTo(editor.config);
		}
		return graph;
	}

	private static NodeLayoutInfo getNodeInfo(JsonArray array, String id) {
		var descriptor = getDescriptor(id);
		if (descriptor != null) {
			for (var elem : array) {
				if (!elem.isJsonObject())
					continue;
				var obj = elem.getAsJsonObject();
				var info = GraphFile.toNodeLayoutInfo(obj);
				if (Objects.equals(info.id, descriptor.refId))
					return info;
			}
		}
		return null;
	}

	public static RootDescriptor getDescriptor(long id) {
		var db = Database.get();
		if (db == null)
			return null;
		for (var c : List.of(Process.class, ProductSystem.class, Result.class)) {
			Descriptor d = db.getDescriptor(c, id);
			if (d instanceof RootDescriptor r)
				return r;
		}
		return null;
	}

	static RootDescriptor getDescriptor(String refId) {
		var db = Database.get();
		if (db == null)
			return null;
		for (var c : List.of(Process.class, ProductSystem.class, Result.class)) {
			Descriptor d = db.getDescriptor(c, refId);
			if (d instanceof RootDescriptor r)
				return r;
		}
		return null;
	}

	public StickyNote createStickyNote(StickyNoteLayoutInfo info) {
		var note = new StickyNote();
		note.setTitle(info.title);
		note.setContent(info.content);
		note.setMinimized(info.minimized);
		note.setSize(info.size);
		note.setLocation(info.location);
		return note;
	}

}
