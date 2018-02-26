package org.openlca.app.editors.graphical.command;

public class ConnectionInput {

	final long sourceId;
	final long flowId;
	final long targetId;
	final long exchangeId;
	final boolean isWaste;
	
	public ConnectionInput(long sourceId, long flowId, long targetId, long exchangeId) {
		this(sourceId, flowId, targetId, exchangeId, false);
	}

	public ConnectionInput(long sourceId, long flowId, long targetId, long exchangeId, boolean isWaste) {
		this.sourceId = sourceId;
		this.flowId = flowId;
		this.targetId = targetId;
		this.exchangeId = exchangeId;
		this.isWaste = isWaste;
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
		if (exchangeId != input.exchangeId)
			return false;
		return true;
	}
}
