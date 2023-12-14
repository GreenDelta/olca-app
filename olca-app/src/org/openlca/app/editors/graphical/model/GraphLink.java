package org.openlca.app.editors.graphical.model;

import org.openlca.app.tools.graphics.model.Component;
import org.openlca.app.tools.graphics.model.Link;
import org.openlca.core.model.ProcessLink;

import static org.openlca.app.tools.graphics.model.Side.INPUT;
import static org.openlca.app.tools.graphics.model.Side.OUTPUT;

/**
 * A model of the connection between two Node objects. The output and the input
 * can be defined by a Node, an IOPane or a ExchangeItem depending on the status
 * of the connection owner (minimized or maximized).
 */
public class GraphLink extends Link {

	public ProcessLink processLink;

	public GraphLink(ProcessLink pLink, Component source, Component target) {
		processLink = pLink;
		reconnect(source, target);
	}

	@Override
	public void reconnect(Component newSource, Component newTarget) {
		super.disconnect();
		source = adaptComponent(newSource, true);
		target = adaptComponent(newTarget, false);
		super.reconnect();

		updateIsExpanded();
	}

	@Override
	public void disconnect() {
		super.disconnect();
		updateIsExpanded();
	}

	@Override
	public void reconnect() {
		super.reconnect();
		updateIsExpanded();
	}

	private void updateIsExpanded() {
		if (getSourceNode() != null)
			getSourceNode().updateIsExpanded(OUTPUT);
		if (getTargetNode() != null)
			getTargetNode().updateIsExpanded(INPUT);
	}

	/**
	 * Getting the deepest representation (in the model tree) of the component.
	 */
	private Component adaptComponent(Component component, boolean isSource) {
		if (component instanceof Node node) {
			if (!node.isMinimized()) {
				ExchangeItem newComponent = isSource
						? node.getOutput(processLink)
						: node.getInput(processLink);
				return newComponent != null ? newComponent : component;
			} else return component;
		}
		if (component instanceof ExchangeItem item)
			if (item.getNode().isMinimized())
				return item.getNode();
		return component;
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

}
