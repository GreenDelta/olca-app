package org.openlca.app.editors.graphical.model;

import static org.openlca.app.components.graphics.model.Side.*;

import org.openlca.app.components.graphics.model.Component;
import org.openlca.app.components.graphics.model.Link;
import org.openlca.core.model.ProcessLink;

/**
 * A model of the connection between two Node objects. The output and the input
 * can be defined by a Node, an IOPane or a ExchangeItem depending on the status
 * of the connection owner (minimized or maximized).
 */
public class GraphLink extends Link {

	public final ProcessLink processLink;

	public GraphLink(ProcessLink pLink, Component source, Component target) {
		this.processLink = pLink;
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
		var s = getSourceNode();
		if (s != null) {
			s.updateIsExpanded(OUTPUT);
		}
		var t = getTargetNode();
		if (t != null) {
			t.updateIsExpanded(INPUT);
		}
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

	@Override
	public Node getSourceNode() {
		return nodeOf(source);
	}

	@Override
	public Node getTargetNode() {
		return nodeOf(target);
	}

	private Node nodeOf(Component c) {
		return switch (c) {
			case Node node -> node;
			case IOPane pane -> pane.getNode();
			case ExchangeItem item -> item.getNode();
			case null, default -> null;
		};
	}

	public ProcessLink getProcessLink() {
		return processLink;
	}

}
