package org.openlca.app.editors.graphical.model;

import java.util.ArrayList;
import java.util.List;

import gnu.trove.set.hash.TLongHashSet;
import org.openlca.app.db.Database;
import org.openlca.app.editors.graphical.GraphConfig;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.search.LinkSearchMap;
import org.openlca.app.tools.graphics.model.BaseComponent;
import org.openlca.app.tools.graphics.model.Component;
import org.openlca.core.matrix.cache.FlowTable;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Process;
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
	public final FlowTable flows = FlowTable.create(Database.get());
	private final TLongHashSet wasteProcesses;
	private final Process referenceProcess;

	public Graph(GraphEditor editor) {
		this.editor = editor;
		var system = editor.getProductSystem();
		this.linkSearch = new LinkSearchMap(system.processLinks);
		referenceProcess = system.referenceProcess;

		wasteProcesses = new TLongHashSet();
		for (var link : system.processLinks) {
			var flowType = flows.type(link.flowId);
			if (flowType == FlowType.WASTE_FLOW) {
				wasteProcesses.add(link.providerId);
			}
		}
	}

	public Node getNode(long id) {
		for (var node : getNodes()) {
			if (node.descriptor != null && node.descriptor.id == id)
				return node;
		}
		return null;
	}

	public Node getReferenceNode() {
		return getNode(referenceProcess.id);
	}

	public boolean isReferenceProcess(Node node) {
		return node != null && referenceProcess.id == node.descriptor.id;
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
