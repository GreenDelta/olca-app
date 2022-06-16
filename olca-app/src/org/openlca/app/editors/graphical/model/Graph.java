package org.openlca.app.editors.graphical.model;


import gnu.trove.set.hash.TLongHashSet;
import org.openlca.app.db.Database;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.search.MutableProcessLinkSearchMap;
import org.openlca.core.matrix.cache.FlowTable;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProductSystem;

import java.util.List;

/**
 * A {@link Graph} renders a system of unit processes, library
 * processes, results and/or product systems (represented by a
 * {@link Node}).
 */
public class Graph extends GraphComponent {

	private double zoom = 1.0;

	public final MutableProcessLinkSearchMap linkSearch;
	public final FlowTable flows = FlowTable.create(Database.get());
	private final TLongHashSet wasteProcesses;
	private final Process referenceProcess;

	public Graph(GraphEditor editor) {
		super(editor);
		var system = editor.getProductSystem();
		this.linkSearch = new MutableProcessLinkSearchMap(system.processLinks);
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
		for (var node : getChildren()) {
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

	public double getZoom() {
		return zoom;
	}

	public void setZoom(double zoom) {
		this.zoom = zoom;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Node> getChildren() {
		return (List<Node>) super.getChildren();
	}

	public ProductSystem getProductSystem() {
		return editor.getProductSystem();
	}

	public String toString() {
		return "GraphModel";
	}

}