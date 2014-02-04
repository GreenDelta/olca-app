package org.openlca.app.results.analysis;

import org.openlca.app.util.Labels;
import org.openlca.core.database.EntityCache;
import org.openlca.core.matrix.LongPair;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.UpstreamTree;
import org.openlca.core.results.UpstreamTreeNode;

import java.util.ArrayList;
import java.util.List;

/**
 * The data model of the sunburst-tree view.
 */
public class SunBurstTree {

	private Node root;

	public static SunBurstTree create(UpstreamTree contributionTree,
	                                  EntityCache cache) {
		SunBurstTree tree = new SunBurstTree();
		UpstreamTreeNode cRoot = contributionTree.getRoot();
		Node root = createNode(cRoot, cache);
		tree.setRoot(root);
		synchChilds(cRoot, root, cache);
		return tree;
	}

	private static Node createNode(UpstreamTreeNode cNode, EntityCache cache) {
		if (cNode == null)
			return null;
		LongPair product = cNode.getProcessProduct();
		if (product == null)
			return null;
		Node node = new Node();
		node.setAmount(cNode.getAmount());
		node.setProcessId(product.getFirst());
		ProcessDescriptor descriptor = cache.get(ProcessDescriptor.class,
				product.getFirst());
		if (descriptor != null)
			node.setProcessName(Labels.getDisplayName(descriptor));
		return node;
	}

	private static void synchChilds(UpstreamTreeNode cNode, Node node,
	                                EntityCache cache) {
		if (cNode == null || node == null)
			return;
		for (UpstreamTreeNode cChild : cNode.getChildren()) {
			if (cChild.getAmount() == 0)
				continue;
			Node child = createNode(cChild, cache);
			if (child == null)
				return;
			node.getChildren().add(child);
			synchChilds(cChild, child, cache);
		}
	}

	public Node getRoot() {
		return root;
	}

	public void setRoot(Node root) {
		this.root = root;
	}

	public static class Node {

		private long processId;
		private String processName;
		private double amount;
		private List<Node> children = new ArrayList<>();

		public long getProcessId() {
			return processId;
		}

		public void setProcessId(long processId) {
			this.processId = processId;
		}

		public String getProcessName() {
			return processName;
		}

		public void setProcessName(String processName) {
			this.processName = processName;
		}

		public double getAmount() {
			return amount;
		}

		public void setAmount(double amount) {
			this.amount = amount;
		}

		public List<Node> getChildren() {
			return children;
		}
	}
}
