package org.openlca.app.editors.results;

import org.eclipse.swt.graphics.Image;
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.ErrorReporter;
import org.openlca.core.model.ResultModel;
import org.openlca.core.model.ResultOrigin;

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

	static Image iconOf(ResultOrigin origin) {
		if (origin == null)
			return null;
		return switch(origin) {
			case CALCULATED -> Icon.FORMULA.get();
			case ENTERED -> Icon.EDIT.get();
			case IMPORTED -> Icon.IMPORT.get();
			case UNKNOWN -> null;
		};
	}
}
