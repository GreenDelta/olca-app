package org.openlca.app.editors.graphical.model;

import org.openlca.core.model.ProcessLink;

/**
 * A model of the connection between two Node objects. The output and the input
 * can be defined by a Node, an IOPane or a ExchangeItem depending on the status
 * of the connection owner (minimized or maximized).
 */
public class Link extends GraphElement {

	public ProcessLink processLink;
	/**
	 * The source and the target can be a Node, an IOPane or a ExchangeItem.
	 */
	protected GraphComponent source, target;
	/** True, if the connection is attached to its endpoints. */
	private boolean isConnected;

	public Link(ProcessLink pLink, GraphComponent source, GraphComponent target) {
		processLink = pLink;
		reconnect(source, target);
	}

	/**
	 * Reconnect to a different input and/or output GraphComponent. The link will
	 * disconnect from its current attachments and reconnect to the new input and
	 * output.
	 */
	public void reconnect(GraphComponent newSource, GraphComponent newTarget) {
		disconnect();

		source = adaptComponent(newSource, true);
		target = adaptComponent(newTarget, false);

		reconnect();
	}

	/**
	 * Getting the deepest representation (in the model tree) of the component.
	 */
	private GraphComponent adaptComponent(GraphComponent component,
		boolean isSource) {
		if (component instanceof Node node) {
			if (!node.isMinimized()) {
				ExchangeItem newComponent = isSource
					? node.getOutput(processLink)
					: node.getInput(processLink);
				return newComponent != null ? newComponent : component;
			}
			else return component;
		}
		if (component instanceof ExchangeItem item)
			if (item.getNode().isMinimized())
				return item.getNode();
		return component;
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

	/**
	 * Returns the provider node of the link which is the source node in case
	 * of a production process and the target node in case of a waste treatment
	 * process.
	 */
	public Node provider() {
		if (processLink == null)
			return null;
		var sourceNode = getSourceNode();
		if (sourceNode != null
			&& sourceNode.descriptor != null
			&& sourceNode.descriptor.id == processLink.providerId)
			return sourceNode;
		var targetNode = getTargetNode();
		if (targetNode != null
			&& targetNode.descriptor != null
			&& targetNode.descriptor.id == processLink.providerId)
			return targetNode;
		return null;
	}

	public Node getSourceNode() {
		if (source instanceof Node node)
			return node;
		else if (source instanceof IOPane pane)
			return pane.getNode();
		else if (source instanceof ExchangeItem item)
			return item.getNode();
		return null;
	}

	public Node getTargetNode() {
		if (target instanceof Node node)
			return node;
		else if (target instanceof IOPane pane)
			return pane.getNode();
		else if (target instanceof ExchangeItem item)
			return item.getNode();
		return null;
	}

	public void setProcessLink(ProcessLink link) {
		processLink = link;
		firePropertyChange("processLink", null, source);
	}

	public ProcessLink getProcessLink() {
		return processLink;
	}

	public GraphComponent getTarget() {
		return target;
	}

	public GraphComponent getSource() {
		return source;
	}

	public boolean isCloseLoop() {
		return getSourceNode() == getTargetNode();
	}

	public String toString() {
		return "Link(" + getSource() + " -> " + getTarget() + ")";
	}

}
