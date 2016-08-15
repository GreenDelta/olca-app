package org.openlca.app.editors.graphical;

import org.openlca.app.editors.graphical.model.ExchangeNode;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.core.model.Flow;
import org.openlca.core.model.descriptors.ProcessDescriptor;

/**
 * Utility functions for the graphical editor.
 */
public final class GraphUtil {

	private GraphUtil() {
	}

	public static ProcessNode getProcessNode(ExchangeNode node) {
		if (node == null || node.getParent() == null)
			return null;
		return node.getParent().getParent();
	}

	public static ProcessDescriptor getProcess(ExchangeNode node) {
		ProcessNode process = getProcessNode(node);
		return process == null ? null : process.getProcess();
	}

	public static Flow getFlow(ExchangeNode node) {
		if (node == null || node.getExchange() == null)
			return null;
		return node.getExchange().getFlow();
	}

	public static ProductSystemNode getSystemNode(ExchangeNode node) {
		ProcessNode process = getProcessNode(node);
		return process == null ? null : process.getParent();
	}

}
