package org.openlca.app.editors.graphical.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openlca.app.db.Database;
import org.openlca.app.editors.graphical.GraphConfig;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.search.LinkSearchMap;
import org.openlca.app.tools.graphics.model.BaseComponent;
import org.openlca.app.tools.graphics.model.Component;
import org.openlca.core.matrix.cache.FlowTable;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;

import static org.eclipse.draw2d.PositionConstants.EAST;


/**
 * A {@link Graph} renders a system of unit processes, library
 * processes, results and/or product systems (represented by a
 * {@link Node}).
 */
public class Graph extends BaseComponent {

	public static final int ORIENTATION = EAST;

	public final GraphEditor editor;
	public final LinkSearchMap linkSearch;
	public Map<ProcessLink, GraphLink> mapProcessLinkToGraphLink = new HashMap<>();
	public final FlowTable flows = FlowTable.create(Database.get());
	private final Process referenceProcess;
	private Node referenceNode;

	public Graph(GraphEditor editor) {
		this.editor = editor;
		var system = editor.getProductSystem();
		this.linkSearch = new LinkSearchMap(system.processLinks);
		referenceProcess = system.referenceProcess;
	}

	public Node getNode(long id) {
		for (var node : getNodes()) {
			if (node.descriptor != null && node.descriptor.id == id)
				return node;
		}
		return null;
	}

	public GraphLink getLink(ProcessLink processLink) {
		return mapProcessLinkToGraphLink.get(processLink);
	}

	/**
	 * Remove graph link from the model graph and process link from the product
	 * system.
	 */
	public void removeLink(ProcessLink link) {
		removeProcessLink(link);
		removeGraphLink(link);
	}

	/**
	 * Remove the ProcessLink from the product system (and not the GraphLink from
	 * the graph).
	 */
	public void removeProcessLink(ProcessLink link) {
		getProductSystem().processLinks.remove(link);
		linkSearch.remove(link);
	}

	/**
	 * Remove the GraphLink from the graph (and not the ProcessLink from the
	 * product system).
	 */
	public GraphLink removeGraphLink(ProcessLink link) {
		var graphLink = mapProcessLinkToGraphLink.remove(link);
		if (graphLink != null) {
			graphLink.disconnect();
		}
		return graphLink;
	}

	public Node getReferenceNode() {
		if (referenceNode == null) {
			referenceNode = getNode(referenceProcess.id);
		}
		return referenceNode;
	}

	public boolean isReferenceProcess(Node node) {
		return node != null && node.equals(getReferenceNode());
	}

	public GraphEditor getEditor() {
		return editor;
	}

	public GraphConfig getConfig() {
		return editor.config;
	}

	@Override
	public Component getFocusComponent() {
		return getReferenceNode();
	}

	/**
	 * @return Only return the Node children of the graph.
	 */
	public List<Node> getNodes() {
		return getChildren().stream()
				.filter(child -> child instanceof Node)
				.map(child -> (Node) child)
				.toList();
	}

	/**
	 * @return Only return the StickyNote children of the graph.
	 */
	public List<StickyNote> getStickyNotes() {
		return getChildren().stream()
				.filter(child -> child instanceof StickyNote)
				.map(child -> (StickyNote) child)
				.toList();
	}

	/**
	 * @return Only return the MinMaxComponent children of the graph.
	 */
	public List<MinMaxComponent> getMinMaxComponents() {
		return getChildren().stream()
				.filter(child -> child instanceof MinMaxComponent)
				.map(child -> (MinMaxComponent) child)
				.toList();
	}

	@Override
	public int compareTo(Component other) {
		return 0;
	}

	public List<Long> getChildrenIds() {
		var ids = new ArrayList<Long>();
		for (var node : getNodes()) {
			ids.add(node.descriptor.id);
		}
		return ids;
	}

	public ProductSystem getProductSystem() {
		return editor.getProductSystem();
	}

	public String toString() {
		return "GraphModel";
	}
}
