package org.openlca.app.results.contributions;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Objects;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openlca.app.util.CostResultDescriptor;
import org.openlca.app.util.Labels;
import org.openlca.core.matrix.index.EnviFlow;
import org.openlca.core.matrix.index.TechFlow;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.results.UpstreamNode;
import org.openlca.core.results.UpstreamTree;
import org.openlca.io.xls.Excel;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.trove.list.array.TDoubleArrayList;

class UpstreamTreeExport implements Runnable {

	/**
	 * The maximum number of levels that should be exported. A value < 0 means
	 * unlimited. In this case reasonable recursion limits are required if the
	 * underlying product system has cycles.
	 */
	public int maxDepth = 10;

	/**
	 * In addition to the maximum tree depth, this parameter describes the minimum
	 * upstream contribution of a node in the tree. A value < 0 also means there is
	 * no minimum contribution.
	 */
	public double minContribution = 1e-9;

	/**
	 * When the max. tree depth is unlimited and the underlying product system has
	 * cycles, this parameter indicates how often a process can occur in a tree
	 * path. It defines the maximum number of expansions of a loop in a tree path.
	 */
	public int maxRecursionDepth = 10;

	private final File file;
	private final UpstreamTree tree;

	private Sheet sheet;
	private int row;
	private int maxColumn;
	private double totalResult;
	private final TDoubleArrayList values = new TDoubleArrayList(100);

	UpstreamTreeExport(File file, UpstreamTree tree) {
		this.file = file;
		this.tree = tree;
	}

	@Override
	public void run() {
		Logger log = LoggerFactory.getLogger(getClass());
		if (file == null || tree == null) {
			log.error("invalid input, file or tree is null");
			return;
		}
		try (var wb = new XSSFWorkbook()) {
			sheet = wb.createSheet("Upstream tree");

			var header = Excel.headerStyle(wb);
			Excel.cell(sheet, 0, 0,
					"Upstream contributions to: " + refName())
					.ifPresent(c -> c.setCellStyle(header));
			Excel.cell(sheet, 1, 0, "Processes")
					.ifPresent(c -> c.setCellStyle(header));

			// write the tree
			row = 1;
			maxColumn = 0;
			totalResult = tree.root.result();
			Path path = new Path(tree.root);
			traverse(path);

			// write the values
			var unit = unit();
			var resultHeader = Strings.nullOrEmpty(unit)
					? "Result"
					: "Result [" + unit + "]";
			Excel.cell(sheet, 1, maxColumn + 1, resultHeader)
					.ifPresent(c -> c.setCellStyle(header));
			for (int i = 0; i < values.size(); i++) {
				Excel.cell(sheet, i + 2, maxColumn + 1, values.get(i));
			}

			// set the column widths
			for (int col = 0; col < maxColumn; col++) {
				sheet.setColumnWidth(col, 750);
			}
			sheet.setColumnWidth(maxColumn, 50 * 255);

			// write the file
			try (var fout = new FileOutputStream(file);
					var buff = new BufferedOutputStream(fout)) {
				wb.write(buff);
			}
		} catch (Exception e) {
			log.error("Tree export failed", e);
			throw new RuntimeException(e);
		}
	}

	private String refName() {
		var ref = tree.ref;
		if (ref == null)
			return "";

		if (ref instanceof EnviFlow) {
			var enviFlow = (EnviFlow) ref;
			if (enviFlow.flow() == null
					|| enviFlow.flow().name == null)
				return "";
			if (enviFlow.location() == null
					|| enviFlow.location().code == null)
				return enviFlow.flow().name;
			return enviFlow.flow().name + " - " + enviFlow.location().code;
		}

		return ref instanceof Descriptor
				? ((Descriptor) ref).name
				: "";
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

	private void traverse(Path path) {

		if (row >= 1048574) {
			// 1048575 is the maximum row number of an
			// Excel sheet.
			return;
		}

		var node = path.node;
		double result = path.node.result();

		// first check if we need to cut the path here
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

		// write the node and expand the child nodes
		write(path);
		for (var child : tree.childs(node)) {
			traverse(path.append(child));
		}
	}

	private void write(Path path) {
		row++;
		values.add(path.node.result());
		int col = path.length;
		maxColumn = Math.max(col, maxColumn);
		var node = path.node;
		if (node.provider() == null
				|| node.provider().provider() == null)
			return;
		var label = Labels.name(node.provider().provider());
		Excel.cell(sheet, row, col, label);
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
