package org.openlca.app.tools.graphics.model;

public class Link extends Element {

	protected Component source, target;

	/** True, if the connection is attached to its endpoints. */
	private boolean isConnected;

	/**
	 * Reconnect to a different input and/or output GraphComponent. The link will
	 * disconnect from its current attachments and reconnect to the new input and
	 * output.
	 */
	public void reconnect(Component newSource, Component newTarget) {
		disconnect();
		source = newSource;
		target = newTarget;
		reconnect();
	}

	/**
	 * Disconnect this link from the component it is attached to.
	 */
	public void disconnect() {
		if (isConnected) {
			target.removeConnection(this);
			source.removeConnection(this);
			isConnected = false;
		}
	}

	/**
	 * Reconnect this Link. The connection will reconnect with the Nodes it was
	 * previously attached to.
	 */
	public void reconnect() {
		if (!isConnected) {
			target.addConnection(this);
			source.addConnection(this);
			isConnected = true;
		}
	}

	public Component getTarget() {
		return target;
	}

	public Component getSource() {
		return source;
	}

	public Component getSourceNode() {
		return source;
	}

	public Component getTargetNode() {
		return target;
	}

	public boolean isCloseLoop() {
		return getSourceNode() == getTargetNode();
	}

}
