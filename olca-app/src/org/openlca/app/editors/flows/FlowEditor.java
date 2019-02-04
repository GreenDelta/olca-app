package org.openlca.app.editors.flows;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.openlca.app.editors.ModelEditor;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowEditor extends ModelEditor<Flow> {

	public static String ID = "editors.flow";
	private Logger log = LoggerFactory.getLogger(getClass());

	public FlowEditor() {
		super(Flow.class);
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		super.init(site, input);
	}

	@Override
	protected void addPages() {
		try {
			addPage(new FlowInfoPage(this));
			addPage(new FlowPropertiesPage(this));
			if (getModel().flowType == FlowType.ELEMENTARY_FLOW) {
				addPage(new ImpactPage(this));
			}
			addCommentPage();
		} catch (Exception e) {
			log.error("failed to add page", e);
		}
	}

}
