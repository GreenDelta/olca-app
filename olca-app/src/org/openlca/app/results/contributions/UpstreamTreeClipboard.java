package org.openlca.app.results.contributions;

import java.util.Objects;

import org.openlca.app.util.CostResultDescriptor;
import org.openlca.app.util.Labels;
import org.openlca.commons.Strings;
import org.openlca.core.matrix.index.EnviFlow;
import org.openlca.core.matrix.index.TechFlow;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.results.UpstreamNode;
import org.openlca.core.results.UpstreamTree;

class UpstreamTreeClipboard {

	/**
	 * Generates a tab-separated text representation of the upstream tree
	 * matching the Excel export format with default options:
	 * - maxDepth: 5
	 * - minContribution: 1e-5
	 * - maxRecursionDepth: 1
	 */
	static String generate(UpstreamTree tree) {
		if (tree == null)
			return "";

		var text = new StringBuilder();
		var exporter = new Exporter(tree, text);
		exporter.export();
		return text.toString();
	}

	private static class Exporter {
		private final UpstreamTree tree;
		private final StringBuilder text;
		private final int maxDepth = 5;
		private final double minContribution = 1e-5;
		private final int maxRecursionDepth = 1;
		private int maxColumn;
		private double totalResult;

		Exporter(UpstreamTree tree, StringBuilder text) {
			this.tree = tree;
			this.text = text;
		}

		void export() {
			// Determine max column first by traversing
			maxColumn = 0;
			totalResult = tree.root.result();
			Path path = new Path(tree.root);
			measureMaxColumn(path);
			int headerMaxColumn = maxColumn;

			// Write header row
			var unit = unit();
			var resultHeader = Strings.isBlank(unit)
					? "Result"
					: "Result [" + unit + "]";
			var directHeader = Strings.isNotBlank(unit)
					? "Direct contribution [" + unit + "]"
					: "Direct contribution";

			// Write headers
			for (int col = 0; col <= headerMaxColumn; col++) {
				if (col > 0) {
					text.append('\t');
				}
				if (col == 0) {
					text.append("Processes");
				}
			}
			text.append('\t').append(resultHeader);
			text.append('\t').append(directHeader);
			text.append('\n');

			// Write tree data
			traverse(path, headerMaxColumn);
		}

		private void measureMaxColumn(Path path) {
			var node = path.node;
			double result = node.result();

			if (result == 0)
				return;
			if (maxDepth > 0 && path.length > maxDepth)
				return;
			if (minContribution > 0 && totalResult != 0) {
				double c = Math.abs(result / totalResult);
				if (c < minContribution)
					return;
			}
			if (maxDepth < 0) {
				int count = path.count(node.provider());
				if (count > maxRecursionDepth) {
					return;
				}
			}

			maxColumn = Math.max(path.length, maxColumn);
			for (var child : tree.childs(node)) {
				measureMaxColumn(path.append(child));
			}
		}

		private void traverse(Path path, int headerMaxColumn) {
			var node = path.node;
			double result = node.result();

			// Check if we need to cut the path here
			if (result == 0)
				return;
			if (maxDepth > 0 && path.length > maxDepth)
				return;
			if (minContribution > 0 && totalResult != 0) {
				double c = Math.abs(result / totalResult);
				if (c < minContribution)
					return;
			}
			if (maxDepth < 0) {
				int count = path.count(node.provider());
				if (count > maxRecursionDepth) {
					return;
				}
			}

			// Write the node
			write(path, headerMaxColumn);
			for (var child : tree.childs(node)) {
				traverse(path.append(child), headerMaxColumn);
			}
		}

		private void write(Path path, int headerMaxColumn) {
			int col = path.length;
			var node = path.node;

			// Write process columns
			for (int i = 0; i <= headerMaxColumn; i++) {
				if (i > 0) {
					text.append('\t');
				}
				if (i == col) {
					if (node.provider() != null
							&& node.provider().provider() != null) {
						text.append(Labels.name(node.provider().provider()));
					}
				}
			}

			// Write result and direct contribution
			text.append('\t').append(Double.toString(node.result()));
			double direct = node.directContribution();
			text.append('\t');
			if (direct != 0) {
				text.append(Double.toString(direct));
			}
			text.append('\n');
		}

		private String unit() {
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

		private static class Path {
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
}

