package org.openlca.app.editors.flows;

import org.openlca.app.editors.ModelEditor;
import org.openlca.core.model.Flow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowEditor extends ModelEditor<Flow> {

	public static String ID = "editors.flow";
	private Logger log = LoggerFactory.getLogger(getClass());

	public FlowEditor() {
		super(Flow.class);
	}

	@Override
	protected void addPages() {
		try {
			addPage(new FlowInfoPage(this));
			addPage(new FlowPropertiesPage(this));
		} catch (Exception e) {
			log.error("failed to add page", e);
		}
	}

}
