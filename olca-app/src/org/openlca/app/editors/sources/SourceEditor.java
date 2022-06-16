package org.openlca.app.editors.sources;

import org.openlca.app.editors.ModelEditor;
import org.openlca.app.util.ErrorReporter;
import org.openlca.core.model.Source;

public class SourceEditor extends ModelEditor<Source> {

	public static String ID = "editors.source";

	public SourceEditor() {
		super(Source.class);
	}

	@Override
	protected void addPages() {
		try {
			addPage(new SourceInfoPage(this));
			addCommentPage();
		} catch (Exception e) {
			ErrorReporter.on("failed to add editor pages", e);
		}
	}

}
