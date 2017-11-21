package org.openlca.app.editors.units;

import org.openlca.app.editors.ModelEditor;
import org.openlca.core.model.UnitGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnitGroupEditor extends ModelEditor<UnitGroup> {

	public static String ID = "editors.unitgroup";
	private Logger log = LoggerFactory.getLogger(getClass());

	public UnitGroupEditor() {
		super(UnitGroup.class);
	}

	@Override
	protected void addPages() {
		try {
			addPage(new UnitGroupInfoPage(this));
			addCommentPage();
		} catch (Exception e) {
			log.error("failed to add editor pages", e);
		}
	}

}
