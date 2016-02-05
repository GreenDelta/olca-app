package org.openlca.app.results.analysis;

import org.openlca.app.results.ExcelExportAction;
import org.openlca.io.xls.results.AnalysisResultExport;
import org.openlca.io.xls.results.IExcelExport;

class ExcelExport extends ExcelExportAction<AnalyzeEditor> {

	@Override
	protected IExcelExport createExport(AnalyzeEditor editor) {
		return new AnalysisResultExport(editor.getSetup(), editor.getResult());
	}

	@Override
	protected String getDefaultFilename() {
		return "analysis_result.xlsx";
	}

}
