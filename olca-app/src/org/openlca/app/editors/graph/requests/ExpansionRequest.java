package org.openlca.app.editors.graph.requests;

import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.openlca.app.editors.graph.model.Node;

import static org.openlca.app.editors.graph.actions.MassExpansionAction.EXPAND;
import static org.openlca.app.editors.graph.model.Node.Side.INPUT;
import static org.openlca.app.editors.graph.model.Node.Side.OUTPUT;
import static org.openlca.app.editors.graph.requests.GraphRequestConstants.*;

public class ExpansionRequest extends Request {

	private final Node node;
	private final int side;

	public ExpansionRequest(Node node, String requestType) {
		this.node = node;
		this.side = INPUT | OUTPUT;
		setType(requestType);
	}

	public ExpansionRequest(Node node, int side) {
		this.node = node;
		this.side = side;
		setType(REQ_EXPAND_OR_COLLAPSE);
	}

	public Node getNode() {
		return node;
	}

	public int getSide() {
		return side;
	}

}
