package org.openlca.app.editors.flow_properties;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.M;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.UI;
import org.openlca.core.model.FlowProperty;

/**
 * Information page of flow properties.
 */
class FlowPropertyInfoPage extends ModelPage<FlowProperty> {

	private FormToolkit toolkit;
	private ScrolledForm form;

	FlowPropertyInfoPage(FlowPropertyEditor editor) {
		super(editor, "FlowPropertyInfoPage", M.GeneralInformation);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		form = UI.formHeader(this);
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		InfoSection infoSection = new InfoSection(getEditor());
		infoSection.render(body, toolkit);
		createAdditionalInfo(infoSection);
		body.setFocus();
		form.reflow(true);
	}

	private void createAdditionalInfo(InfoSection infoSection) {
		link(infoSection.composite(), M.UnitGroup, "unitGroup");
		readOnly(infoSection.composite(), M.FlowPropertyType, "flowPropertyType");
	}
}
