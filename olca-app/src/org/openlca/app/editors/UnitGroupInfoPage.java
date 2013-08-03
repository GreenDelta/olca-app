package org.openlca.app.editors;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.UnitViewer;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.UnitGroup;

public class UnitGroupInfoPage extends ModelPage<UnitGroup> {

	private FormToolkit toolkit;

	public UnitGroupInfoPage(UnitGroupEditor editor) {
		super(editor, "UnitGroupInfoPage", Messages.Common_GeneralInformation);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, Messages.Units_FormText
				+ ": " + getModel().getName());
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		InfoSection infoSection = new InfoSection(getModel(), getBinding());
		infoSection.render(body, toolkit);
		createAdditionalInfo(infoSection, body);
		body.setFocus();
		form.reflow(true);
	}

	protected void createAdditionalInfo(InfoSection infoSection, Composite body) {
		createDropComponent(Messages.Units_DefaultFlowProperty,
				"defaultFlowProperty", ModelType.FLOW_PROPERTY,
				infoSection.getContainer());

		Section section = UI.section(body, toolkit,
				Messages.Units_UnitGroupInfoSectionLabel);
		Composite client = UI.sectionClient(section, toolkit);

		UnitViewer unitViewer = new UnitViewer(client);
		getBinding().on(getModel(), "units", unitViewer);
		unitViewer.bindTo(section);
	}

}
