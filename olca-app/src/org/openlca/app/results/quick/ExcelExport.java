package org.openlca.app.results.quick;

import org.openlca.app.db.Cache;
import org.openlca.app.results.ExcelExportAction;
import org.openlca.io.xls.results.IExcelExport;
import org.openlca.io.xls.results.QuickResultExport;

class ExcelExport extends ExcelExportAction<QuickResultEditor> {

	@Override
	protected IExcelExport createExport(QuickResultEditor editor) {
		return new QuickResultExport(editor.getSetup(), editor.getResult(), Cache.getEntityCache());
	}

	@Override
	protected String getDefaultFilename() {
		return "quick_result.xlsx";
	}

}
