package org.openlca.app.results.contributions;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
import org.openlca.io.xls.Excel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private int resOffset;
	private double totalResult;
	private final ArrayList<PathResult> results = new ArrayList<>();

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

			var bold = Excel.createBoldStyle(wb);
			style(bold, Excel.cell(sheet, 0, 0,
				"Upstream contributions to: " + refName()));
			style(bold, Excel.cell(sheet, 1, 0, "Processes"));

			// first expand the tree, then we know where to write the results
			row = 1;
			resOffset = 0;
			totalResult = tree.root.result();
			Path path = new Path(tree.root);
			traverse(path);

			// write the result values
			writeResultHeader(bold);
			for (int i = 0; i < results.size(); i++) {
				var r = results.get(i);
				Excel.cell(sheet, i + 2, resOffset + 1, r.requiredAmount);
				Excel.cell(sheet, i + 2, resOffset + 2, r.amountUnit);
				Excel.cell(sheet, i + 2, resOffset + 3, r.totalResult);
				var d = r.directResult;
				if (d != 0) {
					Excel.cell(sheet, i + 2, resOffset + 4, d);
				}
			}

			// set the column widths
			for (int col = 0; col < resOffset; col++) {
				sheet.setColumnWidth(col, 750);
			}
			sheet.setColumnWidth(resOffset, 50 * 255);
			sheet.setColumnWidth(resOffset + 1, 25 * 255);
			sheet.setColumnWidth(resOffset + 2, 15 * 255);
			sheet.setColumnWidth(resOffset + 3, 25 * 255);
			sheet.setColumnWidth(resOffset + 4, 25 * 255);

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

	private void writeResultHeader(CellStyle bold) {
		var unit = unit();
		var u = Strings.isBlank(unit)
			? ""
			: " [" + unit + "]";
		style(bold, Excel.cell(sheet, 1, resOffset + 1, "Required amount"));
		style(bold, Excel.cell(sheet, 1, resOffset + 2, "Unit"));
		style(bold, Excel.cell(sheet, 1, resOffset + 3, "Result" + u));
		style(bold, Excel.cell(sheet, 1, resOffset + 4, "Direct contribution" + u));
	}

	private void style(CellStyle style, Optional<Cell> cell) {
		cell.ifPresent(c -> c.setCellStyle(style));
	}

	private String refName() {
		var ref = tree.ref;
		if (ref == null)
			return "";
		if (ref instanceof EnviFlow enviFlow)
			return Labels.name(enviFlow);
		return ref instanceof Descriptor
			? ((Descriptor) ref).name
			: "";
	}

	private String unit() {
		var ref = tree.ref;
		return switch (ref) {
			case EnviFlow f -> Labels.refUnit(f);
			case FlowDescriptor f -> Labels.refUnit(f);
			case ImpactDescriptor i -> i.referenceUnit;
			case CostResultDescriptor ignored -> Labels.getReferenceCurrencyCode();
			case null, default -> "";
		};
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
		results.add(PathResult.of(path));
		int col = path.length;
		resOffset = Math.max(col, resOffset);
		var node = path.node;
		var label = Labels.name(node.provider());
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

	private record PathResult(
		double totalResult,
		double directResult,
		double requiredAmount,
		String amountUnit
	) {

		static PathResult of(Path path) {
			var node = path.node;
			return new PathResult(
				node.result(),
				node.directContribution(),
				node.requiredAmount(),
				Labels.refUnit(node.provider())
			);
		}
	}

}
