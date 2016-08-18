package org.openlca.app.editors.graphical;

import java.util.Collections;
import java.util.List;

import org.openlca.app.editors.graphical.model.ExchangeNode;
import org.openlca.app.editors.graphical.model.InputOutputNode;
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

	public static ProductSystemGraphEditor getEditor(ExchangeNode node) {
		ProductSystemNode system = getSystemNode(node);
		return system == null ? null : system.getEditor();
	}

	public static List<ExchangeNode> getExchangeNodes(ProcessNode node) {
		if (node == null || node.getChildren() == null)
			return Collections.emptyList();
		List<InputOutputNode> ioList = node.getChildren();
		if (ioList.isEmpty())
			return Collections.emptyList();
		InputOutputNode io = ioList.get(0);
		return io.getChildren() == null
				? Collections.emptyList()
				: io.getChildren();
	}

}
