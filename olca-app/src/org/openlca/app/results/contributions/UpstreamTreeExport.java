package org.openlca.app.results.contributions;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openlca.app.util.Labels;
import org.openlca.commons.Strings;
import org.openlca.core.results.UpstreamTree;
import org.openlca.io.xls.Excel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.trove.list.array.TDoubleArrayList;

class UpstreamTreeExport extends UpstreamTreeTraversal implements Runnable {

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

	private Sheet sheet;
	private int row;
	private int maxColumn;
	private final TDoubleArrayList results = new TDoubleArrayList(100);
	private final TDoubleArrayList direct = new TDoubleArrayList(100);

	UpstreamTreeExport(File file, UpstreamTree tree) {
		super(tree);
		this.file = file;
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
			Excel.cell(sheet, 0, 0,
							"Upstream contributions to: " + refName())
					.ifPresent(c -> c.setCellStyle(bold));
			Excel.cell(sheet, 1, 0, "Processes")
					.ifPresent(c -> c.setCellStyle(bold));

			// write the tree
			row = 1;
			maxColumn = 0;
			Path path = new Path(tree.root);
			traverse(path);

			// write the result values
			var unit = unit();
			var resultHeader = Strings.isBlank(unit)
					? "Result"
					: "Result [" + unit + "]";
			var directHeader = Strings.isNotBlank(unit)
					? "Direct contribution [" + unit + "]"
					: "Direct contribution";
			Excel.cell(sheet, 1, maxColumn + 1, resultHeader)
					.ifPresent(c -> c.setCellStyle(bold));
			Excel.cell(sheet, 1, maxColumn + 2, directHeader)
					.ifPresent(c -> c.setCellStyle(bold));
			for (int i = 0; i < results.size(); i++) {
				Excel.cell(sheet, i + 2, maxColumn + 1, results.get(i));
				var d = direct.get(i);
				if (d != 0) {
					Excel.cell(sheet, i + 2, maxColumn + 2, d);
				}
			}

			// set the column widths
			for (int col = 0; col < maxColumn; col++) {
				sheet.setColumnWidth(col, 750);
			}
			sheet.setColumnWidth(maxColumn, 50 * 255);
			sheet.setColumnWidth(maxColumn + 1, 25 * 255);
			sheet.setColumnWidth(maxColumn + 2, 25 * 255);

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

	private void traverse(Path path) {
		if (row >= 1048574) {
			// 1048575 is the maximum row number of an Excel sheet.
			return;
		}

		if (!shouldInclude(path))
			return;

		// write the node and expand the child nodes
		write(path);
		for (var child : tree.childs(path.node)) {
			traverse(path.append(child));
		}
	}

	private void write(Path path) {
		row++;
		results.add(path.node.result());
		direct.add(path.node.directContribution());
		int col = path.length;
		maxColumn = Math.max(col, maxColumn);
		var node = path.node;
		if (node.provider() == null
				|| node.provider().provider() == null)
			return;
		var label = Labels.name(node.provider().provider());
		Excel.cell(sheet, row, col, label);
	}


}
