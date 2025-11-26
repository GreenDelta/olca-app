package org.openlca.app.editors.sd.editor.graph.model;

import org.openlca.app.components.graphics.model.Component;
import org.openlca.app.components.graphics.model.Link;

/**
 * Represents a connection (link) between two nodes in the system dynamics graph.
 * Links can represent:
 * - Flow connections between stocks (through rates)
 * - Information links (auxiliary to rate, stock to auxiliary, etc.)
 */
public class SdLink extends Link {

	/**
	 * True if this is a flow connection (material/energy flow between stocks),
	 * false if it's an information link.
	 */
	private boolean isFlowConnection;

	public SdLink(Component source, Component target, boolean isFlowConnection) {
		this.source = source;
		this.target = target;
		this.isFlowConnection = isFlowConnection;
	}

	/**
	 * Connect this link to its source and target nodes.
	 */
	public void connect() {
		reconnect();
	}

	/**
	 * Returns true if this is a flow connection (material flow),
	 * false if it's an information link.
	 */
	public boolean isFlowConnection() {
		return isFlowConnection;
	}

	public void setFlowConnection(boolean flowConnection) {
		this.isFlowConnection = flowConnection;
	}

	@Override
	public SdNode getSourceNode() {
		if (source instanceof SdNode node) {
			return node;
		}
		return null;
	}

	@Override
	public SdNode getTargetNode() {
		if (target instanceof SdNode node) {
			return node;
		}
		return null;
	}

	// TODO: Bind this link to the actual connection in the SD model
	// This would reference the dependency between variables in the XMILE model
}
