package org.openlca.app.results.analysis;

import java.util.ArrayList;
import java.util.List;

import org.openlca.app.util.Labels;
import org.openlca.core.database.EntityCache;
import org.openlca.core.matrix.LongPair;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.UpstreamTree;
import org.openlca.core.results.UpstreamTreeNode;

/**
 * The data model of the sunburst-tree view.
 */
public class SunBurstTree {

	private static final double EPSILON = 1e-12;

	/** The total amount of the tree. */
	@SuppressWarnings("unused")
	private double amount;

	/** The unit of the values in the tree nodes. */
	@SuppressWarnings("unused")
	private String unit;

	/**
	 * The top-level children of the sun-burst tree. This list usally contains
	 * only the reference process.
	 */
	private List<Node> children = new ArrayList<>();

	public static SunBurstTree create(UpstreamTree contributionTree,
			EntityCache cache) {
		SunBurstTree tree = new SunBurstTree();
		UpstreamTreeNode cRoot = contributionTree.getRoot();
		tree.amount = cRoot.getAmount();
		tree.unit = getUnit(contributionTree, cache);
		Node root = createNode(cRoot, cache);
		synchChilds(cRoot, root, cache);
		if (root != null)
			tree.children.add(root);
		return tree;
	}

	private static String getUnit(UpstreamTree tree, EntityCache cache) {
		if (tree == null || tree.getReference() == null)
			return "";
		BaseDescriptor ref = tree.getReference();
		if (ref instanceof ImpactCategoryDescriptor)
			return ((ImpactCategoryDescriptor) ref).getReferenceUnit();
		if (ref instanceof FlowDescriptor)
			return Labels.getRefUnit((FlowDescriptor) ref, cache);
		else
			return "";
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
			if (Math.abs(cChild.getAmount()) < EPSILON)
				continue;
			Node child = createNode(cChild, cache);
			if (child == null)
				return;
			node.getChildren().add(child);
			synchChilds(cChild, child, cache);
		}
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
