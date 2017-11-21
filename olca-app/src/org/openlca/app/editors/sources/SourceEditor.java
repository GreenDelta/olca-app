package org.openlca.app.editors.sources;

import org.openlca.app.editors.ModelEditor;
import org.openlca.core.model.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SourceEditor extends ModelEditor<Source> {

	public static String ID = "editors.source";
	private Logger log = LoggerFactory.getLogger(getClass());

	public SourceEditor() {
		super(Source.class);
	}

	@Override
	protected void addPages() {
		try {
			addPage(new SourceInfoPage(this));
			addCommentPage();
		} catch (Exception e) {
			log.error("failed to add editor pages", e);
		}
	}

}
