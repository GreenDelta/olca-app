package org.openlca.app.editors.flows;

import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.db.Cache;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.UI;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.util.Strings;

class FlowPropertiesPage extends ModelPage<Flow> {

	private FormToolkit toolkit;
	private FlowEditor editor;

	FlowPropertiesPage(FlowEditor editor) {
		super(editor, "FlowPropertiesPage", Messages.FlowProperties);
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, Messages.Flow + ": "
				+ getModel().getName());
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		Section section = UI.section(body, toolkit, Messages.FlowProperties);
		UI.gridData(section, true, true);
		Composite client = UI.sectionClient(section, toolkit);
		FlowPropertyFactorViewer viewer = new FlowPropertyFactorViewer(client,
				Cache.getEntityCache(), editor);
		setInitialInput(viewer);
		viewer.bindTo(section);
		editor.onSaved(() -> viewer.setInput(getModel()));
		body.setFocus();
		form.reflow(true);
	}

	private void setInitialInput(FlowPropertyFactorViewer viewer) {
		List<FlowPropertyFactor> factors = getModel().getFlowPropertyFactors();
		factors.sort((f1, f2) -> {
			FlowProperty prop1 = f1.getFlowProperty();
			FlowProperty prop2 = f2.getFlowProperty();
			if (prop1 == null || prop2 == null)
				return 0;
			return Strings.compare(prop1.getName(), prop2.getName());
		});
		viewer.setInput(factors);
	}

}
