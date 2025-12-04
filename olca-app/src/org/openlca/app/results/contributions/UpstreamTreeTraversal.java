package org.openlca.app.results.contributions;

import java.util.Objects;

import org.openlca.app.util.CostResultDescriptor;
import org.openlca.app.util.Labels;
import org.openlca.core.matrix.index.EnviFlow;
import org.openlca.core.matrix.index.TechFlow;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.results.UpstreamNode;
import org.openlca.core.results.UpstreamTree;

/**
 * Base class for traversing and exporting UpstreamTree structures.
 * Contains shared logic for tree traversal, filtering, and path management.
 */
abstract class UpstreamTreeTraversal {

	protected final UpstreamTree tree;
	protected int maxDepth;
	protected double minContribution;
	protected int maxRecursionDepth;
	protected double totalResult;

	protected UpstreamTreeTraversal(UpstreamTree tree) {
		this.tree = tree;
		this.totalResult = tree != null ? tree.root.result() : 0;
	}

	/**
	 * Checks if a node should be included based on filtering criteria.
	 */
	protected boolean shouldInclude(Path path) {
		var node = path.node;
		double result = node.result();

		if (result == 0)
			return false;
		if (maxDepth > 0 && path.length > maxDepth)
			return false;
		if (minContribution > 0 && totalResult != 0) {
			double c = Math.abs(result / totalResult);
			if (c < minContribution)
				return false;
		}
		if (maxDepth < 0) {
			int count = path.count(node.provider());
			if (count > maxRecursionDepth) {
				return false;
			}
		}
		return true;
	}

	protected String refName() {
		var ref = tree.ref;
		if (ref == null)
			return "";
		if (ref instanceof EnviFlow enviFlow)
			return Labels.name(enviFlow);
		return ref instanceof Descriptor
				? ((Descriptor) ref).name
				: "";
	}

	protected String unit() {
		var ref = tree.ref;
		if (ref == null)
			return "";

		if (ref instanceof EnviFlow)
			return Labels.refUnit((EnviFlow) ref);

		if (ref instanceof FlowDescriptor)
			return Labels.refUnit((FlowDescriptor) ref);

		if (ref instanceof ImpactDescriptor)
			return ((ImpactDescriptor) ref).referenceUnit;

		if (ref instanceof CostResultDescriptor)
			return Labels.getReferenceCurrencyCode();

		return "";
	}

	/**
	 * Represents a path through the upstream tree.
	 */
	protected static class Path {
		final Path prefix;
		final UpstreamNode node;
		final int length;

		Path(UpstreamNode node) {
			this.prefix = null;
			this.node = node;
			this.length = 0;
		}

		Path(UpstreamNode node, Path prefix) {
			this.prefix = prefix;
			this.node = node;
			this.length = 1 + prefix.length;
		}

		Path append(UpstreamNode node) {
			return new Path(node, this);
		}

		int count(TechFlow techFlow) {
			int c = Objects.equals(techFlow, node.provider()) ? 1 : 0;
			return prefix != null ? c + prefix.count(techFlow) : c;
		}
	}
}

