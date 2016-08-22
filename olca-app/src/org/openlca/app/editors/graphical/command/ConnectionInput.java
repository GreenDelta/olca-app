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

	@Override
	public boolean equals(Object arg0) {
		if (!(arg0 instanceof ConnectionInput))
			return false;
		if (this == arg0)
			return true;
		ConnectionInput input = (ConnectionInput) arg0;
		if (sourceId != input.sourceId)
			return false;
		if (targetId != input.targetId)
			return false;
		if (flowId != input.flowId)
			return false;
		return true;
	}
}
