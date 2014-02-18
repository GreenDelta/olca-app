package org.openlca.app.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.util.Images;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.LocationViewer;
import org.openlca.core.model.Flow;

class FlowInfoPage extends ModelPage<Flow> {

	private FormToolkit toolkit;

	FlowInfoPage(FlowEditor editor) {
		super(editor, "FlowInfoPage", Messages.GeneralInformation);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, Messages.Flow + ": "
				+ getModel().getName());
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		InfoSection infoSection = new InfoSection(getModel(), getBinding());
		infoSection.render(body, toolkit);
		FlowUseSection useSection = new FlowUseSection(getModel(),
				Database.get());
		useSection.render(body, toolkit);
		createAdditionalInfo(infoSection, body);
		body.setFocus();
		form.reflow(true);
	}

	private void createAdditionalInfo(InfoSection infoSection, Composite body) {
		createCheckBox(Messages.IsInfrastructureFlow, "infrastructureFlow",
				infoSection.getContainer());
		createReadOnly(Messages.FlowType,
				Images.getIcon(getModel().getFlowType()), "flowType",
				infoSection.getContainer());
		Composite composite = UI.formSection(body, toolkit,
				Messages.AdditionalInfo);
		createText(Messages.CasNumber, "casNumber", composite);
		createText(Messages.Formula, "formula", composite);
		createLocationViewer(composite);
	}

	private void createLocationViewer(Composite composite) {
		new Label(composite, SWT.NONE).setText(Messages.Location);
		LocationViewer viewer = new LocationViewer(composite);
		viewer.setNullable(true);
		viewer.setInput(Database.get());
		getBinding().on(getModel(), "location", viewer);
	}
}
