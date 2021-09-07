package org.openlca.app.editors.results;

import org.openlca.app.editors.ModelEditor;
import org.openlca.app.util.ErrorReporter;
import org.openlca.core.model.ResultModel;

public class ResultEditor extends ModelEditor<ResultModel> {

	public ResultEditor() {
		super(ResultModel.class);
	}

	@Override
	protected void addPages() {
		try {
			addPage(new ResultPage(this));
		} catch (Exception e) {
			ErrorReporter.on("Failed to open result", e);
		}
	}
}
