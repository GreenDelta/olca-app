package org.openlca.app.editors.units;

import org.openlca.app.editors.ModelEditor;
import org.openlca.app.util.ErrorReporter;
import org.openlca.core.model.UnitGroup;

public class UnitGroupEditor extends ModelEditor<UnitGroup> {

	public static String ID = "editors.unitgroup";

	public UnitGroupEditor() {
		super(UnitGroup.class);
	}

	@Override
	protected void addPages() {
		try {
			addPage(new UnitGroupInfoPage(this));
			addCommentPage();
		} catch (Exception e) {
			ErrorReporter.on("failed to add editor pages", e);
		}
	}

}
