package org.openlca.app.editors.flow_properties;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.Messages;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.preferencepages.FeatureFlag;
import org.openlca.app.util.Editors;
import org.openlca.app.util.UI;
import org.openlca.core.model.FlowProperty;

/**
 * Information page of flow properties.
 */
class FlowPropertyInfoPage extends ModelPage<FlowProperty> {

	private FormToolkit toolkit;

	FlowPropertyInfoPage(FlowPropertyEditor editor) {
		super(editor, "FlowPropertyInfoPage", Messages.GeneralInformation);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, Messages.FlowProperty
				+ ": " + getModel().getName());
		if (FeatureFlag.SHOW_REFRESH_BUTTONS.isEnabled())
			Editors.addRefresh(form, getEditor());
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		InfoSection infoSection = new InfoSection(getEditor());
		infoSection.render(body, toolkit);
		createAdditionalInfo(infoSection);
		body.setFocus();
		form.reflow(true);
	}

	private void createAdditionalInfo(InfoSection infoSection) {
		createLink(Messages.UnitGroup, "unitGroup", infoSection.getContainer());
		createReadOnly(Messages.FlowPropertyType, "flowPropertyType",
				infoSection.getContainer());
	}
}
