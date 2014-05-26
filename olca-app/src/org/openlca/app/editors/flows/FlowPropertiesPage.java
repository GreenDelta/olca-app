package org.openlca.app.editors.flows;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.db.Cache;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.table.FlowPropertyFactorViewer;
import org.openlca.core.model.Flow;

class FlowPropertiesPage extends ModelPage<Flow> {

	private FormToolkit toolkit;

	FlowPropertiesPage(FlowEditor editor) {
		super(editor, "FlowPropertiesPage", Messages.FlowPropertiesPageLabel);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, Messages.Flow + ": "
				+ getModel().getName());
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		Section section = UI.section(body, toolkit,
				Messages.FlowPropertiesPageLabel);
		UI.gridData(section, true, true);
		Composite client = UI.sectionClient(section, toolkit);
		FlowPropertyFactorViewer factorViewer = new FlowPropertyFactorViewer(
				client, Cache.getEntityCache());
		getBinding().onList(() -> getModel(), "flowPropertyFactors",
				factorViewer);
		factorViewer.bindTo(section);
		body.setFocus();
		form.reflow(true);
	}

}
