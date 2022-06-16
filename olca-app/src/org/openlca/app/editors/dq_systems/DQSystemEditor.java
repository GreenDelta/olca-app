package org.openlca.app.editors.dq_systems;

import org.openlca.app.editors.ModelEditor;
import org.openlca.app.util.ErrorReporter;
import org.openlca.core.model.DQSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DQSystemEditor extends ModelEditor<DQSystem> {

	public static String ID = "editors.dqsystem";
	private DQSystemInfoPage infoPage;

	public DQSystemEditor() {
		super(DQSystem.class);
	}

	@Override
	protected void addPages() {
		try {
			addPage(infoPage = new DQSystemInfoPage(this));
			addCommentPage();
		} catch (Exception e) {
			ErrorReporter.on("Failed to init editor");
		}
	}

	@Override
	protected void doAfterUpdate() {
		super.doAfterUpdate();
		infoPage.redraw();
	}

}
