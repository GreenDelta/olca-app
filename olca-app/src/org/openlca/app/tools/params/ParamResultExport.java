package org.openlca.app.tools.params;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openlca.app.util.Labels;
import org.openlca.core.model.ParameterRedef;
import org.openlca.io.xls.Excel;
import org.openlca.util.Strings;

class ParamResultExport implements Runnable {

	private final ParamResult result;
	private final File target;
	private Throwable err;
	private int row = 0;

	ParamResultExport(ParamResult result, File target) {
		this.result = result;
		this.target = target;
	}

	@Override
	public void run() {
		try (var wb = new XSSFWorkbook()) {
			var sheet = wb.createSheet("Results");
			writeSetupSection(sheet);
			writeParameterSection(sheet);
			writeResultSection(sheet);
			sheet.autoSizeColumn(0);
			sheet.autoSizeColumn(1);
			try (var out = new FileOutputStream(target)) {
				wb.write(out);
			}
		} catch (Exception e) {
			this.err = e;
		}
	}

	Throwable error() {
		return err;
	}

	private void writeSetupSection(Sheet sheet) {
		Excel.cell(sheet, row++, 0, "Calculation setup");
		Excel.cell(sheet, row, 0, "Product system");
		Excel.cell(sheet, row++, 1, Labels.name(result.system()));
		Excel.cell(sheet, row, 0, "Impact assessment method");
		Excel.cell(sheet, row++, 1, Labels.name(result.method()));
		Excel.cell(sheet, row, 0, "Allocation method");
		Excel.cell(sheet, row++, 1, Labels.of(result.allocation()));
		Excel.cell(sheet, row, 0, "Number of iterations");
		Excel.cell(sheet, row++, 1, result.count());
		row++;
	}

	private void writeParameterSection(Sheet sheet) {
		Excel.cell(sheet, row++, 0, "Parameters");
		Excel.cell(sheet, row, 0, "Name");
		Excel.cell(sheet, row, 1, "Context");
		Excel.cell(sheet, row, 2, "Start value");
		Excel.cell(sheet, row, 3, "End value");
		Excel.cell(sheet, row, 5, "Iteration");
		int headerRow = row;
		row++;

		var paramRows = new HashMap<String, Integer>();
		for (var param : result.seq().params()) {
			var redef = param.redef;
			var key = keyOf(redef);
			paramRows.put(key, row);
			Excel.cell(sheet, row, 0, redef.name);
			var context = param.context != null
					? Labels.name(param.context)
					: "global";
			Excel.cell(sheet, row, 1, context);
			Excel.cell(sheet, row, 2, param.start);
			Excel.cell(sheet, row, 3, param.end);
			row++;
		}
		row++;

		for (int i = 0; i < result.count(); i++) {
			int col = 6 + i;
			Excel.cell(sheet, headerRow, col, i + 1);
			for (var redef : result.seq().get(i)) {
				var paramRow = paramRows.get(keyOf(redef));
				if (paramRow == null)
					continue;
				Excel.cell(sheet, paramRow, col, redef.value);
			}
		}
	}

	private String keyOf(ParameterRedef redef) {
		if (redef == null)
			return "";
		return redef.contextId == null
				? redef.name
				: redef.name + "/" + redef.contextId;
	}

	private void writeResultSection(Sheet sheet) {
		Excel.cell(sheet, row++, 0, "Impact assessment results");
		Excel.cell(sheet, row, 0, "Impact category");
		Excel.cell(sheet, row, 1, "Unit");
		Excel.cell(sheet, row, 5, "Iteration");
		for (int i = 0; i < result.count(); i++) {
			Excel.cell(sheet, row, 6 + i, i + 1);
		}
		row++;

		var impacts = new ArrayList<>(result.impacts());
		impacts.sort(
				(i1, i2) -> Strings.compareIgnoreCase(Labels.name(i1), Labels.name(i2)));
		for (var impact : impacts) {
			Excel.cell(sheet, row, 0, Labels.name(impact));
			Excel.cell(sheet, row, 1, impact.referenceUnit);
			var values = result.seriesOf(impact);
			for (int i = 0; i < values.length; i++) {
				Excel.cell(sheet, row, 6 + i, values[i]);
			}
			row++;
		}
	}
}
