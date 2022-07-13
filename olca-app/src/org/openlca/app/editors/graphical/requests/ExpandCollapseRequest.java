package org.openlca.app.editors.graphical.requests;

import org.eclipse.gef.Request;
import org.openlca.app.editors.graphical.model.Node;

import static org.openlca.app.editors.graphical.model.Node.Side.INPUT;
import static org.openlca.app.editors.graphical.model.Node.Side.OUTPUT;
import static org.openlca.app.editors.graphical.requests.GraphRequestConstants.*;

public class ExpandCollapseRequest extends Request {

	private final Node node;
	private final int side;

	public ExpandCollapseRequest(Node node, String requestType) {
		this.node = node;
		this.side = INPUT | OUTPUT;
		setType(requestType);
	}

	public ExpandCollapseRequest(Node node, int side) {
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
