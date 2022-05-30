package org.openlca.app.editors.graph.requests;

import org.eclipse.gef.Request;
import org.openlca.app.editors.graph.model.Node;

public class ExpansionRequest extends Request {

	public static String REQ_EXPANSION = "collapse";

	private final Node node;
	private final Node.Side side;

	public ExpansionRequest(Node node, Node.Side side) {
		this.node = node;
		this.side = side;
		setType(REQ_EXPANSION);
	}

	public Node getNode() {
		return node;
	}

	public Node.Side getSide() {
		return side;
	}
}
