package org.openlca.app.editors.sd.results;

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

			var numericVars = Util.numericVarsOf(vars);
			if (numericVars.isEmpty()) {
				Excel.cell(sheet, 0, 0, "No numeric variables found");
				return write(wb);
			}

			writeHeaders(wb, numericVars, sheet);
			writeValueRows(numericVars, sheet);

			// Auto-size all columns for better readability
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
		for (int j = 0; j < numericVars.size(); j++) {
			var var = numericVars.get(j);
			var values = var.values();
			for (int i = 0; i < values.size(); i++) {
				var value = values.get(i);
				if (value instanceof NumCell(double num)) {
					Excel.cell(sheet, i + 1, j + 1, num);
				}
			}
		}
	}

	private void writeHeaders(Workbook wb, List<Var> numVars, Sheet sheet) {
		var style = Excel.createBoldStyle(wb);
		Excel.cell(sheet, 0, 0, "Iteration")
				.ifPresent(cell -> cell.setCellStyle(style));
		for (int i = 0; i < numVars.size(); i++) {
			var var = numVars.get(i);
			Excel.cell(sheet, 0, i + 1, var.name().label())
					.ifPresent(cell -> cell.setCellStyle(style));
		}

		int iterations = numVars.stream()
				.mapToInt(v -> v.values().size())
				.max()
				.orElse(0);
		for (int row = 1; row <= iterations; row++) {
			Excel.cell(sheet, row, 0, row)
					.ifPresent(cell -> cell.setCellStyle(style));
		}
	}


}
