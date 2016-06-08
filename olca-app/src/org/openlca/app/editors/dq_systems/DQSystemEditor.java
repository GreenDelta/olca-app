package org.openlca.app.editors.dq_systems;

import org.openlca.app.editors.IEditor;
import org.openlca.app.editors.ModelEditor;
import org.openlca.core.model.DQSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DQSystemEditor extends ModelEditor<DQSystem> implements IEditor {

	public static String ID = "editors.dqsystem";
	private Logger log = LoggerFactory.getLogger(getClass());
	private DQSystemInfoPage infoPage;
	
	public DQSystemEditor() {
		super(DQSystem.class);
	}

	@Override
	protected void addPages() {
		try {
			addPage(infoPage = new DQSystemInfoPage(this));
		} catch (Exception e) {
			log.error("failed to add page", e);
		}
	}
	
	@Override
	protected void doAfterUpdate() {
		super.doAfterUpdate();
		infoPage.redraw();
	}

}
