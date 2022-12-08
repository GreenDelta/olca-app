package org.openlca.app.editors.graphical.requests;

import org.eclipse.gef.Request;
import org.openlca.app.editors.graphical.model.Node;

import static org.openlca.app.editors.graphical.requests.GraphRequestConstants.*;
import static org.openlca.app.tools.graphics.model.Side.INPUT;
import static org.openlca.app.tools.graphics.model.Side.OUTPUT;

public class ExpandCollapseRequest extends Request {

	private final Node node;
	private final int side;
	private final boolean quiet;

	public ExpandCollapseRequest(Node node, String requestType, boolean quiet) {
		this.node = node;
		this.side = INPUT | OUTPUT;
		this.quiet = quiet;
		setType(requestType);
	}

	public ExpandCollapseRequest(Node node, int side, boolean quiet) {
		this.node = node;
		this.side = side;
		this.quiet = quiet;
		setType(REQ_EXPAND_OR_COLLAPSE);
	}

	public Node getNode() {
		return node;
	}

	public int getSide() {
		return side;
	}

	public boolean isQuiet() {
		return quiet;
	}

}
