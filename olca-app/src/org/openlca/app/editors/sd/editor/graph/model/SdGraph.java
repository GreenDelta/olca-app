package org.openlca.app.editors.sd.editor.graph.model;

import java.util.ArrayList;
import java.util.List;

import org.openlca.app.components.graphics.model.BaseComponent;
import org.openlca.app.components.graphics.model.Component;
import org.openlca.app.editors.sd.editor.graph.SdGraphEditor;

/**
 * The root model for the system dynamics graph editor.
 * Contains all stocks, rates, auxiliaries, and their connections.
 */
public class SdGraph extends BaseComponent {

	public final SdGraphEditor editor;

	public SdGraph(SdGraphEditor editor) {
		this.editor = editor;
	}

	/**
	 * Returns all nodes (stocks, rates, auxiliaries) in this graph.
	 */
	public List<SdNode> getNodes() {
		var nodes = new ArrayList<SdNode>();
		for (var child : getChildren()) {
			if (child instanceof SdNode node) {
				nodes.add(node);
			}
		}
		return nodes;
	}

	/**
	 * Find a node by its variable name.
	 */
	public SdNode findNode(String variableName) {
		for (var node : getNodes()) {
			if (node.getVariableName().equals(variableName)) {
				return node;
			}
		}
		return null;
	}

	/**
	 * Get all links in the graph.
	 */
	public List<SdLink> getLinks() {
		var links = new ArrayList<SdLink>();
		for (var node : getNodes()) {
			for (var link : node.getSourceConnections()) {
				if (link instanceof SdLink sdLink) {
					links.add(sdLink);
				}
			}
		}
		return links;
	}

	/**
	 * Add a node to the graph.
	 */
	public void addNode(SdNode node) {
		addChild(node);
	}

	/**
	 * Remove a node from the graph. Also disconnects all links.
	 */
	public void removeNode(SdNode node) {
		// Disconnect all links first
		var linksToRemove = new ArrayList<>(node.getAllConnections());
		for (var link : linksToRemove) {
			if (link instanceof SdLink sdLink) {
				sdLink.disconnect();
			}
		}
		removeChild(node);
	}

	@Override
	public Component getFocusComponent() {
		// TODO: Return the focused/selected component if needed
		return null;
	}

	@Override
	public int compareTo(Component other) {
		// TODO Auto-generated method stub
		return 0;
	}
}
