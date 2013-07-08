package org.openlca.app.editors;

import org.openlca.core.editors.ModelEditor;
import org.openlca.core.editors.ModelEditorPage;

public class UnitGroupEditor extends ModelEditor {

	public static String ID = "UnitGroupEditor";

	@Override
	protected ModelEditorPage[] initPages() {
		return new ModelEditorPage[] { new UnitGroupInfoPage(this) };
	}
}
