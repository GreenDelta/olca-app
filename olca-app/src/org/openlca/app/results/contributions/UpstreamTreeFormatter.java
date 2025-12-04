package org.openlca.app.results.contributions;

import java.util.List;

import org.openlca.app.util.Labels;
import org.openlca.commons.Strings;
import org.openlca.core.results.UpstreamNode;
import org.openlca.core.results.UpstreamTree;

class UpstreamTreeFormatter {

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

	/**
	 * Generates a tab-separated text representation for selected nodes
	 * matching the Excel export format with default options.
	 * Each selected node is treated as a root (level 0) and its subtree
	 * is formatted in Excel-like structure.
	 */
	static String generateForNodes(UpstreamTree tree, List<UpstreamNode> selectedNodes) {
		if (tree == null || selectedNodes == null || selectedNodes.isEmpty())
			return "";

		var text = new StringBuilder();
		var exporter = new Exporter(tree, text);
		exporter.exportForNodes(selectedNodes);
		return text.toString();
	}

	private static class Exporter extends UpstreamTreeTraversal {
		private final StringBuilder text;
		private int maxColumn;

		Exporter(UpstreamTree tree, StringBuilder text) {
			super(tree);
			this.text = text;
			this.maxDepth = 5;
			this.minContribution = 1e-5;
			this.maxRecursionDepth = 1;
		}

		void export() {
			// Determine max column first by traversing
			maxColumn = 0;
			Path path = new Path(tree.root);
			measureMaxColumn(path);
			int headerMaxColumn = maxColumn;

			// Write header row
			writeHeaders(headerMaxColumn);

			// Write tree data
			traverse(path, headerMaxColumn);
		}

		void exportForNodes(List<UpstreamNode> selectedNodes) {
			// Determine max column first by traversing all selected nodes
			maxColumn = 0;
			for (var node : selectedNodes) {
				Path path = new Path(node);
				measureMaxColumn(path);
			}
			int headerMaxColumn = maxColumn;

			// Write header row
			writeHeaders(headerMaxColumn);

			// Write tree data for each selected node
			for (var node : selectedNodes) {
				Path path = new Path(node);
				traverse(path, headerMaxColumn);
			}
		}

		private void writeHeaders(int headerMaxColumn) {
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
		}

		private void measureMaxColumn(Path path) {
			if (!shouldInclude(path))
				return;

			maxColumn = Math.max(path.length, maxColumn);
			for (var child : tree.childs(path.node)) {
				measureMaxColumn(path.append(child));
			}
		}

		private void traverse(Path path, int headerMaxColumn) {
			if (!shouldInclude(path))
				return;

			// Write the node
			write(path, headerMaxColumn);
			for (var child : tree.childs(path.node)) {
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

	}
}

