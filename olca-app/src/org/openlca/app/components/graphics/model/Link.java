package org.openlca.app.components.graphics.model;

import java.util.Objects;

public abstract class Link extends PropertyNotifier {

	protected Component source;
	protected Component target;

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

	public abstract Component getSourceNode();

	public abstract Component getTargetNode();

	public boolean isSelfLoop() {
		return Objects.equals(getSourceNode(), getTargetNode());
	}

	@Override
	public String toString() {
		return "Link(" + getSource() + " -> " + getTarget() + ")";
	}

}
