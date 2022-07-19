package org.openlca.app.editors.graphical.model;

import java.util.*;

import com.google.gson.JsonArray;
import org.eclipse.draw2d.geometry.Point;
import org.openlca.app.db.Database;
import org.openlca.app.editors.graphical.GraphConfig;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.GraphFile;
import org.openlca.app.editors.graphical.layouts.NodeLayoutInfo;
import org.openlca.app.editors.graphical.model.commands.ExpandCommand;
import org.openlca.app.util.Labels;
import org.openlca.core.model.*;
import org.openlca.core.model.Process;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.util.Strings;

import static org.openlca.app.editors.graphical.model.GraphComponent.INPUT_PROP;
import static org.openlca.app.editors.graphical.model.GraphComponent.OUTPUT_PROP;
import static org.openlca.app.editors.graphical.model.Node.Side.INPUT;
import static org.openlca.app.editors.graphical.model.Node.Side.OUTPUT;

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

		var node = applyInfo(new Node(descriptor, editor), info);

		// A Node (MinMaxGraphComponent) `minimized` attribute is by default true.
		if (!node.isMinimized()) {
			var panes = createIOPanes(node.descriptor);
			node.addChild(panes.get(INPUT_PROP), 0);
			node.addChild(panes.get(OUTPUT_PROP), 1);
		}

		return node;
	}

	/**
	 * Mutate <code>node</code> with the <code>NodeLayoutInfo</code> provided.
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

	public HashMap<String, IOPane> createIOPanes(RootDescriptor descriptor) {
		var panes = new HashMap<String, IOPane>();
		panes.put(INPUT_PROP, new IOPane(editor, true));
		panes.put(OUTPUT_PROP, new IOPane(editor, false));

		var exchanges = getExchanges(descriptor);

		// filter and sort the exchanges
		exchanges.stream()
			.filter(e -> {
				if (e.flow == null)
					return false;
				return editor.config.showElementaryFlows()
					|| e.flow.flowType != FlowType.ELEMENTARY_FLOW;
			})
			.sorted((e1, e2) -> {
				int t1 = typeOrderOf(e1);
				int t2 = typeOrderOf(e2);
				if (t1 != t2)
					return t1 - t2;
				var name1 = Labels.name(e1.flow);
				var name2 = Labels.name(e2.flow);
				return Strings.compare(name1, name2);
			})
			.forEach(e -> {
				var key = e.isInput ? INPUT_PROP : OUTPUT_PROP;
				panes.get(key).addChild(new ExchangeItem(editor, e));
			});

		return panes;
	}

	private List<Exchange> getExchanges(RootDescriptor descriptor) {
		return switch (descriptor.type) {
			case PROCESS -> {
				var process = Database.get().get(Process.class, descriptor.id);
				yield process == null
					? Collections.emptyList()
					: process.exchanges;
			}
			case PRODUCT_SYSTEM -> {
				var system = Database.get().get(ProductSystem.class, descriptor.id);
				yield system == null || system.referenceExchange == null
					? Collections.emptyList()
					: Collections.singletonList(system.referenceExchange);
			}
			case RESULT -> {
				var result = Database.get().get(Result.class, descriptor.id);
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

	private int typeOrderOf(Exchange e) {
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

	public static void createNecessaryLinks(Graph graph) {
		var linkSearch = graph.linkSearch;
		for (ProcessLink pLink : linkSearch.getLinks(graph.getChildrenIds())) {
			var providerNode = graph.getNode(pLink.providerId);
			var consumerNode = graph.getNode(pLink.processId);
			if (providerNode == null || consumerNode == null)
				continue;
			FlowType type = graph.flows.type(pLink.flowId);
			var outNode = type == FlowType.PRODUCT_FLOW ? providerNode
				: type == FlowType.WASTE_FLOW ? consumerNode
				: null;
			var inNode = type == FlowType.PRODUCT_FLOW ? consumerNode
				: type == FlowType.WASTE_FLOW ? providerNode
				: null;
			if (inNode == null)
				continue;
			new Link(pLink, outNode, inNode);
		}
	}

	public Graph createGraph(GraphEditor editor, JsonArray array) {
		if (array == null)
			return createGraph(editor);

		var graph = new Graph(editor);

		var system = editor.getProductSystem();
		var referenceProcess = system.referenceProcess;

		// Create the reference node.
		if (referenceProcess != null) {
			var refNodeInfo = getNodeInfo(array, referenceProcess.refId);
			var descriptor = getDescriptor(referenceProcess.id);
			var refNode = createNode(descriptor, refNodeInfo);
			if (refNode != null) {
				graph.addChild(refNode);
			}
		}

		// Create other nodes.
		for (var elem : array) {
			if (!elem.isJsonObject())
				continue;
			var obj = elem.getAsJsonObject();
			var info = GraphFile.toInfo(obj);
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

		createNecessaryLinks(graph);
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
					var command = new ExpandCommand(refNode, side);
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
				var info = GraphFile.toInfo(obj);
				if (Objects.equals(info.id, descriptor.refId))
					return info;
			}
		}
		return null;
	}

	// We changed the ID to a string in openLCA v2; to be a bit backwards
	// compatible we try the long ID too.
	public static NodeLayoutInfo getNodeInfo(JsonArray array, long id) {
		return getNodeInfo(array, String.valueOf(id));
	}

	public static RootDescriptor getDescriptor(long id) {
		var db = Database.get();
		if (db == null)
			return null;
		for (var c : List.of(Process.class, ProductSystem.class, Result.class)) {
			if (db.getDescriptor(c, id) instanceof RootDescriptor d)
				return d;
		}
		return null;
	}

	static RootDescriptor getDescriptor(String refId) {
		var db = Database.get();
		if (db == null)
			return null;
		for (var c : List.of(Process.class, ProductSystem.class, Result.class)) {
			if (db.getDescriptor(c, refId) instanceof RootDescriptor d)
				return d;
		}
		return null;
	}

}
