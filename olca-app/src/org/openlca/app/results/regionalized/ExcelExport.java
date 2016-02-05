package org.openlca.app.results.regionalized;

import org.openlca.app.results.ExcelExportAction;
import org.openlca.io.xls.results.AnalysisResultExport;
import org.openlca.io.xls.results.IExcelExport;

class ExcelExport extends ExcelExportAction<RegionalizedResultEditor> {

	@Override
	protected IExcelExport createExport(RegionalizedResultEditor editor) {
		return new AnalysisResultExport(editor.getSetup(), editor.getResult());
	}

	@Override
	protected String getDefaultFilename() {
		return "regionalized_analysis_result.xlsx";
	}

}
