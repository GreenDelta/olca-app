package org.openlca.core.editors.io;

import org.apache.poi.ss.usermodel.Sheet;
import org.openlca.core.model.Process;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.results.AnalysisResult;

/**
 * Writes the total impact assessment result of an analysis result to an Excel
 * sheet. The total impact assessment result is the upstream total result of the
 * reference process.
 */
class AnalysisTotalImpact {

	private Sheet sheet;
	private AnalysisResult result;
	private AnalysisResultExport export;

	private AnalysisTotalImpact(Sheet sheet, AnalysisResult result,
			AnalysisResultExport export) {
		this.sheet = sheet;
		this.result = result;
		this.export = export;
	}

	public static void write(Sheet sheet, AnalysisResult result,
			AnalysisResultExport export) {
		new AnalysisTotalImpact(sheet, result, export).doIt();
	}

	private void doIt() {
		int col = export.IMPACT_INFO_SIZE + 1;
		export.writeImpactRowHeader(sheet, 1);
		export.header(sheet, 1, col, "Result");
		int row = 2;
		Process refProcess = result.getSetup().getReferenceProcess();
		for (ImpactCategoryDescriptor impact : export.getImpacts()) {
			export.writeImpactRowInfo(sheet, row, impact);
			double val = result.getResult(refProcess, impact);
			Excel.cell(sheet, row, col, val);
			row++;
		}
		// Excel.autoSize(sheet, 1, 2, 3, 4);
	}

}
