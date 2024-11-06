package org.openlca.app.editors.flow_properties;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.UI;
import org.openlca.core.model.FlowProperty;

/**
 * Information page of flow properties.
 */
class FlowPropertyInfoPage extends ModelPage<FlowProperty> {

	FlowPropertyInfoPage(FlowPropertyEditor editor) {
		super(editor, "FlowPropertyInfoPage", M.GeneralInformation);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		var form = UI.header(this);
		var tk = managedForm.getToolkit();
		Composite body = UI.body(form, tk);
		InfoSection infoSection = new InfoSection(getEditor());
		infoSection.render(body, tk);
		createAdditionalInfo(infoSection);
		body.setFocus();
		form.reflow(true);
	}

	private void createAdditionalInfo(InfoSection infoSection) {
		link(infoSection.composite(), M.UnitGroup, "unitGroup");
		readOnly(infoSection.composite(), M.FlowPropertyType, "flowPropertyType");
	}
}
