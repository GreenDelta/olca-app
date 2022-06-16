package org.openlca.app.editors.flows;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.util.ErrorReporter;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowType;

public class FlowEditor extends ModelEditor<Flow> {

	public static String ID = "editors.flow";

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
			ErrorReporter.on("failed to add page", e);
		}
	}

}
