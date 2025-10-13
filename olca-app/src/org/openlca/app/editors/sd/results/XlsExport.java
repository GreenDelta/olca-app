package org.openlca.app.editors.sd.results;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openlca.commons.Res;
import org.openlca.commons.Strings;
import org.openlca.io.xls.Excel;
import org.openlca.sd.eqn.Tensor;
import org.openlca.sd.eqn.Var;
import org.openlca.sd.eqn.cells.BoolCell;
import org.openlca.sd.eqn.cells.Cell;
import org.openlca.sd.eqn.cells.EqnCell;
import org.openlca.sd.eqn.cells.NumCell;
import org.openlca.sd.eqn.cells.TensorCell;
import org.openlca.sd.util.Tensors;

class XlsExport {

	private final List<Var> vars;
	private final File file;
	private final Map<String, Integer> columns;
	private final int iterCount;

	private XlsExport(List<Var> vars, File file) {
		this.vars = new ArrayList<>(vars);
		this.vars.sort((vi, vj) -> Strings.compareIgnoreCase(
				vi.name().value(), vj.name().label()));
		this.file = file;
		this.columns = new HashMap<>();
		iterCount = vars.stream()
				.mapToInt(v -> v.values().size())
				.max()
				.orElse(0);
	}

	static Res<Void> run(List<Var> vars, File file) {
		if (file == null)
			return Res.error("No valid file provided");
		if (vars == null || vars.isEmpty())
			return Res.error("No simulation variables found");
		return new XlsExport(vars, file).doIt();
	}

	private Res<Void> doIt() {
		try (var wb = new XSSFWorkbook()) {
			var sheet = wb.createSheet("Simulation variables");
			var headers = initColumns();
			writeHeaders(wb, headers, sheet);
			writeValueRows(sheet);
			sheet.autoSizeColumn(0);
			try (var out = new FileOutputStream(file)) {
				wb.write(out);
			}
			return Res.ok();
		} catch (Exception e) {
			return Res.error("failed to write simulation result", e);
		}
	}

	private String[] initColumns() {
		int col = 0;
		var headers = new ArrayList<String>();
		for (var v : vars) {
			var val = v.value();
			if (val instanceof TensorCell(Tensor t)) {
				var addresses = Tensors.addressesOf(t);
				for (var address : addresses) {
					var key = Tensors.addressKeyOf(v, address);
					headers.add(key);
					columns.put(key, col++);
				}
			} else {
				var key = v.name().value();
				headers.add(key);
				columns.put(key, col++);
			}
		}
		return headers.toArray(String[]::new);
	}



	private void writeValueRows(Sheet sheet) {

		for (int i = 0; i < iterCount; i++) {
			for (var v : vars) {

				var values = v.values();
				if (i >= values.size())
					continue;

				var value = values.get(i);
				if (value instanceof TensorCell(Tensor t)) {
					var addresses = Tensors.addressesOf(t);
					for (var address : addresses) {
						var key = Tensors.addressKeyOf(v, address);
						var col = columns.get(key);
						if (col != null) {
							put(sheet, i + 1, col + 1, t.get(address));
						}
					}
				} else {
					var key = v.name().value();
					var col = columns.get(key);
					if (col != null) {
						put(sheet, i + 1, col + 1, value);
					}
				}
			}
		}
	}

	private void put(Sheet sheet, int row, int col, Cell cell) {
		switch (cell) {
			case NumCell(double num) -> Excel.cell(sheet, row, col, num);
			case BoolCell(boolean bool) -> Excel.cell(sheet, row, col, bool);
			case EqnCell(String eqn) -> Excel.cell(sheet, row, col, eqn);
			case null, default -> {
			}
		}
	}

	private void writeHeaders(Workbook wb, String[] headers, Sheet sheet) {
		var style = Excel.createBoldStyle(wb);

		// column headers
		Excel.cell(sheet, 0, 0, "Iteration")
				.ifPresent(cell -> cell.setCellStyle(style));
		for (int i = 0; i < headers.length; i++) {
			Excel.cell(sheet, 0, i + 1, headers[i])
					.ifPresent(cell -> cell.setCellStyle(style));
		}

		// iteration numbers
		for (int row = 1; row <= iterCount; row++) {
			Excel.cell(sheet, row, 0, row)
					.ifPresent(cell -> cell.setCellStyle(style));
		}
	}
}
