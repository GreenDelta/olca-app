package org.openlca.app.editors.sd;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openlca.io.xls.Excel;
import org.openlca.sd.eqn.Cell.NumCell;
import org.openlca.sd.eqn.Var;
import org.openlca.util.Res;
import org.openlca.util.Strings;

class SdResultExport {

	private final List<Var> vars;
	private final File file;

	private SdResultExport(List<Var> vars, File file) {
		this.vars = vars;
		this.file = file;
	}

	static Res<Void> run(List<Var> vars, File file) {
		return new SdResultExport(vars, file).doIt();
	}

	private Res<Void> doIt() {
		try (var wb = new XSSFWorkbook()) {
			var sheet = wb.createSheet("Simulation results");

			var numericVars = getNumericVars();
			if (numericVars.isEmpty()) {
				Excel.cell(sheet, 0, 0, "No numeric variables found");
				return write(wb);
			}

			writeHeaderRow(wb, numericVars, sheet);
			writeValueRows(numericVars, sheet);
			sheet.autoSizeColumn(0);

			return write(wb);
		} catch (Exception e) {
			return Res.error("failed to write simulation result", e);
		}
	}

	private Res<Void> write(Workbook wb) throws IOException {
		try (var out = new FileOutputStream(file)) {
			wb.write(out);
		}
		return Res.VOID;
	}

	private void writeValueRows(List<Var> numericVars, Sheet sheet) {
		int row = 1;
		for (var var : numericVars) {
			Excel.cell(sheet, row, 0, var.name().value());
			var values = var.values();
			for (int i = 0; i < values.size(); i++) {
				var value = values.get(i);
				if (value instanceof NumCell(double num)) {
					Excel.cell(sheet, row, i + 1, num);
				} else if (value != null) {
					Excel.cell(sheet, row, i + 1, " - ");
				}
			}
			row++;
		}
	}

	private void writeHeaderRow(Workbook wb, List<Var> numericVars, Sheet sheet) {
		var headerStyle = Excel.createBoldStyle(wb);
		int maxIterations = numericVars.stream()
				.mapToInt(v -> v.values().size())
				.max()
				.orElse(0);
		Excel.cell(sheet, 0, 0, "Variable / Iteration")
				.ifPresent(cell -> cell.setCellStyle(headerStyle));
		for (int i = 0; i < maxIterations; i++) {
			Excel.cell(sheet, 0, i + 1, i + 1)
					.ifPresent(cell -> cell.setCellStyle(headerStyle));
		}
	}

	private List<Var> getNumericVars() {
		return vars.stream()
				.filter(v -> v.value() instanceof NumCell)
				.sorted((vi, vj) -> {
					var ni = vi.name().label();
					var nj = vj.name().label();
					return Strings.compare(ni, nj);
				})
				.toList();
	}

}
