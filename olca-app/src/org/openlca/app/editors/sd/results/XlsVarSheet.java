package org.openlca.app.editors.sd.results;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.openlca.app.editors.sd.interop.CoupledResult;
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

class XlsVarSheet {

	private final List<Var> vars;
	private final Sheet sheet;
	private final CellStyle bold;

	private final Map<String, Integer> columns;
	private final int iterCount;

	private XlsVarSheet(Sheet sheet, CellStyle bold, List<Var> vars) {
		this.sheet = sheet;
		this.bold = bold;
		this.vars = vars;
		this.columns = new HashMap<>();
		iterCount = vars.stream()
			.mapToInt(v -> v.values().size())
			.max()
			.orElse(0);
	}

	static void create(Workbook wb, CoupledResult result) {
		if (wb == null || result == null)
			return;
		var vars = new ArrayList<>(result.vars());
		vars.sort((vi, vj) -> Strings.compareIgnoreCase(
			vi.name().value(), vj.name().label()));
		var sheet = wb.createSheet("Simulation variables");
		var bold = Excel.createBoldStyle(wb);
		new XlsVarSheet(sheet, bold, vars).write();
	}

	private void write() {
		var headers = initColumns();
		writeHeaders(headers);
		writeValueRows();
		sheet.autoSizeColumn(0);
	}

	private void writeHeaders(String[] headers) {

		// column headers
		Excel.cell(sheet, 0, 0, "Iteration")
			.ifPresent(cell -> cell.setCellStyle(bold));
		for (int i = 0; i < headers.length; i++) {
			Excel.cell(sheet, 0, i + 1, headers[i])
				.ifPresent(cell -> cell.setCellStyle(bold));
		}

		// iteration numbers
		for (int row = 1; row <= iterCount; row++) {
			Excel.cell(sheet, row, 0, row)
				.ifPresent(cell -> cell.setCellStyle(bold));
		}
	}

	private void writeValueRows() {

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
							put(i + 1, col + 1, t.get(address));
						}
					}
				} else {
					var key = v.name().value();
					var col = columns.get(key);
					if (col != null) {
						put(i + 1, col + 1, value);
					}
				}
			}
		}
	}

	private void put(int row, int col, Cell cell) {
		switch (cell) {
			case NumCell(double num) -> Excel.cell(sheet, row, col, num);
			case BoolCell(boolean bool) -> Excel.cell(sheet, row, col, bool);
			case EqnCell(String eqn) -> Excel.cell(sheet, row, col, eqn);
			case null, default -> {
			}
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
}
