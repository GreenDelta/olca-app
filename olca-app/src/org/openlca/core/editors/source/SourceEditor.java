package org.openlca.core.editors.source;

import org.openlca.core.editors.ModelEditor;
import org.openlca.core.editors.ModelEditorPage;

public class SourceEditor extends ModelEditor {

	public static String ID = "SourceEditor";

	@Override
	protected ModelEditorPage[] initPages() {
		return new ModelEditorPage[] { new SourceInfoPage(this) };
	}

}
