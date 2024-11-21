package org.openlca.app.results.slca.ui;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openlca.app.results.slca.SocialRiskValue;
import org.openlca.core.model.RiskLevel;
import org.openlca.io.xls.Excel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SocialTreeExport implements Runnable {

	private final File file;
	private final TreeModel tree;
	private Sheet sheet;

	private int row;
	private int maxColumn;

	private final List<String> activityValues = new ArrayList<>();
	private final List<String> rawValues = new ArrayList<>();
	private final List<SocialRiskValue> socialRiskValues = new ArrayList<>();

	SocialTreeExport(File file, TreeModel tree) {
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
			sheet = wb.createSheet("Social assessment");

			var bold = Excel.createBoldStyle(wb);
			Excel.cell(sheet, 0, 0, "Social assessment")
					.ifPresent(c -> c.setCellStyle(bold));

			// write the tree
			row = 1;
			var roots = tree.getElements(tree);
			for (var root : roots) {
				if (root instanceof TreeModel.Node n) {
					traverse(new Path(n));
				}
			}

			// write the result
			Excel.cell(sheet, 1, maxColumn + 1, "Activity value")
					.ifPresent(c -> c.setCellStyle(bold));
			Excel.cell(sheet, 1, maxColumn + 2, "Raw value")
					.ifPresent(c -> c.setCellStyle(bold));
			for (int j = 0; j < RiskLevel.values().length; j++) {
				var level = RiskLevel.values()[j];
				Excel.cell(sheet, 1, maxColumn + j + 3, TreeGrid.headerOf(level))
						.ifPresent(c -> c.setCellStyle(bold));
			}
			for (int i = 0; i < socialRiskValues.size(); i++) {
				Excel.cell(sheet, i + 2, maxColumn + 1, activityValues.get(i));
				Excel.cell(sheet, i + 2, maxColumn + 2, rawValues.get(i));
				for (var level : RiskLevel.values()) {
					var j = level.ordinal();
					var share = socialRiskValues.get(i).getShare(level);
					Excel.cell(sheet, i + 2, maxColumn + j + 3, share);
				}
			}

			// set the column widths
			for (int col = 0; col < maxColumn + 1; col++) {
				sheet.setColumnWidth(col, 25 * 255);
			}
			sheet.setColumnWidth(maxColumn + 1, 20 * 255);
			sheet.setColumnWidth(maxColumn + 2, 20 * 255);
			for (int j = 0; j < RiskLevel.values().length; j++) {
				sheet.setColumnWidth(maxColumn + j + 3, 10 * 255);
			}

			try (var fout = new FileOutputStream(file);
					 var buff = new BufferedOutputStream(fout)) {
				wb.write(buff);
			}
		} catch (Exception e) {
			log.error("Social assessment export failed", e);
			throw new RuntimeException(e);
		}
	}

	private void traverse(Path path) {
		if (row >= 1048574) {
			// 1048575 is the maximum row number of an
			// Excel sheet.
			return;
		}

		// write the node and expand the child nodes
		var node = path.node;
		write(path);
		for (var child : tree.getChildren(node)) {
			if (child instanceof TreeModel.Node n) {
				traverse(path.append(n));
			}
		}
	}

	private void write(Path path) {
		row++;

		activityValues.add(path.node.activityValue());
		rawValues.add(path.node.rawValue());
		socialRiskValues.add(path.node.riskValue());

		int col = path.length;
		maxColumn = Math.max(col, maxColumn);
		var node = path.node;
		Excel.cell(sheet, row, col, node.name());
	}

	private static class Path {
		final Path prefix;
		final TreeModel.Node node;
		final int length;

		Path(TreeModel.Node node) {
			this.prefix = null;
			this.node = node;
			this.length = 0;
		}

		Path(TreeModel.Node node, Path prefix) {
			this.prefix = prefix;
			this.node = node;
			this.length = 1 + prefix.length;
		}

		Path append(TreeModel.Node node) {
			return new Path(node, this);
		}

	}

}
