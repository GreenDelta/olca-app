package org.openlca.core.editors.io;

import org.apache.poi.ss.usermodel.Sheet;
import org.openlca.core.model.Process;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.results.AnalysisResult;

/**
 * Writes the single process impact assessment contributions to an Excel sheet.
 * The export format is a matrix where the processes are listed in the rows and
 * the impact assessment categories in the columns.
 */
class AnalysisProcessImpacts {

	private Sheet sheet;
	private AnalysisResult result;
	private AnalysisResultExport export;

	private int startRow;
	private int startCol;

	private AnalysisProcessImpacts(Sheet sheet, AnalysisResult result,
			AnalysisResultExport export) {
		this.sheet = sheet;
		this.result = result;
		this.export = export;
		startRow = export.IMPACT_INFO_SIZE + 1;
		startCol = export.PROCESS_INFO_SIZE;
	}

	public static void write(Sheet sheet, AnalysisResult result,
			AnalysisResultExport export) {
		new AnalysisProcessImpacts(sheet, result, export).doIt();
	}

	private void doIt() {
		export.writeImpactColHeader(sheet, startCol);
		export.writeProcessRowHeader(sheet, startRow);
		int col = startCol + 1;
		for (ImpactCategoryDescriptor impact : export.getImpacts()) {
			export.writeImpactColInfo(sheet, col, impact);
			int row = startRow + 1;
			for (Process process : export.getProcesses()) {
				export.writeProcessRowInfo(sheet, row, process);
				double val = result.getSingleResult(process, impact);
				Excel.cell(sheet, row, col, val);
				row++;
			}
			col++;
		}
		// Excel.autoSize(sheet, 1, 2, 3, 4);
	}
}
