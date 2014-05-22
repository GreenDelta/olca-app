package org.openlca.app.editors.graphical.command;

public class ConnectionInput {

	long sourceId;
	long targetId;
	long flowId;

	public ConnectionInput(long sourceId, long targetId, long flowId) {
		this.sourceId = sourceId;
		this.targetId = targetId;
		this.flowId = flowId;
	}

}
