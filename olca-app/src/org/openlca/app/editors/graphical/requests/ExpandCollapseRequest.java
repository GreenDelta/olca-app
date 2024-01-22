package org.openlca.app.editors.graphical.requests;

import org.eclipse.gef.Request;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.app.tools.graphics.model.Side;

import static org.openlca.app.editors.graphical.requests.GraphRequestConstants.*;

public class ExpandCollapseRequest extends Request {

	private final Node node;
	private final Side side;
	private final boolean quiet;

	public ExpandCollapseRequest(Node node, String requestType, boolean quiet) {
		this.node = node;
		this.side = Side.BOTH;
		this.quiet = quiet;
		setType(requestType);
	}

	public ExpandCollapseRequest(Node node, Side side, boolean quiet) {
		this.node = node;
		this.side = side;
		this.quiet = quiet;
		setType(REQ_EXPAND_OR_COLLAPSE);
	}

	public Node getNode() {
		return node;
	}

	public Side getSide() {
		return side;
	}

	public boolean isQuiet() {
		return quiet;
	}

}
