package org.openlca.app.editors.results;

import org.openlca.app.editors.ModelEditor;
import org.openlca.app.util.ErrorReporter;
import org.openlca.core.model.Result;

public class ResultEditor extends ModelEditor<Result> {

	public ResultEditor() {
		super(Result.class);
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
