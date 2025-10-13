package org.openlca.app.editors.sd.results;

import java.io.File;
import java.io.FileOutputStream;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openlca.app.editors.sd.interop.CoupledResult;
import org.openlca.commons.Res;
import org.openlca.io.xls.Excel;

class XlsExport {

	private final CoupledResult result;
	private final File file;

	private XlsExport(CoupledResult result, File file) {
		this.result = result;
		this.file = file;
	}

	static Res<Void> run(CoupledResult result, File file) {
		if (file == null)
			return Res.error("No valid file provided.");
		if (result == null || result.size() == 0)
			return Res.error("The simulation result is empty.");
		return new XlsExport(result, file).doIt();
	}

	private Res<Void> doIt() {
		try (var wb = new XSSFWorkbook()) {
			if (result.hasImpactResults()) {
				var sheet = wb.createSheet("Impact assessment results");
				var bold = Excel.createBoldStyle(wb);
				writeImpacts(sheet, bold);
			}

			XlsVarSheet.create(wb, result);

			try (var out = new FileOutputStream(file)) {
				wb.write(out);
			}
			return Res.ok();
		} catch (Exception e) {
			return Res.error("failed to write simulation result", e);
		}
	}

	private void writeImpacts(Sheet sheet, CellStyle bold) {
		Excel.cell(sheet, 0, 0, "Iteration")
			.ifPresent(cell -> cell.setCellStyle(bold));

		int col = 0;
		for (var d : result.getImpactCategories()) {
			col++;
			Excel.cell(sheet, 0, col, d.name + " [" + d.referenceUnit + "]")
				.ifPresent(cell -> cell.setCellStyle(bold));
			var values = result.impactResultsOf(d);
			if (values == null)
				continue;
			for (int i = 0; i < values.length; i++) {
				if (col == 1) {
					Excel.cell(sheet, i + 1, 0, i + 1)
						.ifPresent(cell -> cell.setCellStyle(bold));
				}
				Excel.cell(sheet, i + 1, col, values[i]);
			}
		}

		sheet.autoSizeColumn(0);
	}
}
