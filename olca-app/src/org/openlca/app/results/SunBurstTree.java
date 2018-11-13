package org.openlca.app.results;

import java.util.ArrayList;
import java.util.List;

import org.openlca.app.util.Labels;
import org.openlca.core.database.EntityCache;
import org.openlca.core.matrix.LongPair;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.UpstreamNode;
import org.openlca.core.results.UpstreamTree;

/**
 * The data model of the sunburst-tree view.
 */
public class SunBurstTree {

	private static final double EPSILON = 1e-12;

	private UpstreamTree uTree;

	/** The total amount of the tree. */
	@SuppressWarnings("unused")
	private double amount;

	/** The unit of the values in the tree nodes. */
	@SuppressWarnings("unused")
	private String unit;

	/**
	 * The top-level children of the sun-burst tree. This list usually contains
	 * only the reference process.
	 */
	private List<Node> children = new ArrayList<>();

	public static SunBurstTree create(UpstreamTree uTree, EntityCache cache) {
		SunBurstTree tree = new SunBurstTree();
		tree.uTree = uTree;
		UpstreamNode cRoot = uTree.root;
		tree.amount = cRoot.result;
		tree.unit = getUnit(uTree, cache);
		Node root = tree.createNode(cRoot, cache);
		tree.synchChilds(cRoot, root, cache);
		if (root != null)
			tree.children.add(root);
		return tree;
	}

	private static String getUnit(UpstreamTree tree, EntityCache cache) {
		if (tree == null || tree.ref == null)
			return "";
		BaseDescriptor ref = tree.ref;
		if (ref instanceof ImpactCategoryDescriptor)
			return ((ImpactCategoryDescriptor) ref).getReferenceUnit();
		if (ref instanceof FlowDescriptor)
			return Labels.getRefUnit((FlowDescriptor) ref, cache);
		else
			return "";
	}

	private Node createNode(UpstreamNode uNode, EntityCache cache) {
		if (uNode == null)
			return null;
		LongPair product = uNode.provider;
		if (product == null)
			return null;
		Node node = new Node();
		node.amount = uNode.result;
		node.processId = product.getFirst();
		ProcessDescriptor descriptor = cache.get(
				ProcessDescriptor.class, product.getFirst());
		if (descriptor != null)
			node.processName = Labels.getDisplayName(descriptor);
		return node;
	}

	private void synchChilds(UpstreamNode uNode, Node node,
			EntityCache cache) {
		if (uNode == null || node == null)
			return;
		for (UpstreamNode uChild : uTree.childs(uNode)) {
			if (Math.abs(uChild.result) < EPSILON)
				continue;
			Node child = createNode(uChild, cache);
			if (child == null)
				return;
			node.children.add(child);
			synchChilds(uChild, child, cache);
		}
	}

	public static class Node {

		public long processId;
		public String processName;
		public double amount;
		public List<Node> children = new ArrayList<>();
	}
}
