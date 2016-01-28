package org.openlca.app.editors.flows;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.preferencepages.FeatureFlag;
import org.openlca.app.util.Editors;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.LocationViewer;
import org.openlca.core.model.Flow;

class FlowInfoPage extends ModelPage<Flow> {

	private FormToolkit toolkit;
	private ScrolledForm form;

	FlowInfoPage(FlowEditor editor) {
		super(editor, "FlowInfoPage", M.GeneralInformation);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		form = UI.formHeader(managedForm);
		updateFormTitle();
		if (FeatureFlag.SHOW_REFRESH_BUTTONS.isEnabled())
			Editors.addRefresh(form, getEditor());
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		InfoSection infoSection = new InfoSection(getEditor());
		infoSection.render(body, toolkit);
		FlowUseSection useSection = new FlowUseSection(getModel(),
				Database.get());
		useSection.render(body, toolkit);
		createAdditionalInfo(infoSection, body);
		body.setFocus();
		form.reflow(true);
	}

	@Override
	protected void updateFormTitle() {
		if (form == null)
			return;
		form.setText(M.Flow + ": " + getModel().getName());
	}

	private void createAdditionalInfo(InfoSection infoSection, Composite body) {
		createCheckBox(M.InfrastructureFlow, "infrastructureFlow",
				infoSection.getContainer());
		createReadOnly(M.FlowType,
				Images.get(getModel()), "flowType",
				infoSection.getContainer());
		Composite composite = UI.formSection(body, toolkit,
				M.AdditionalInformation);
		createText(M.CASNumber, "casNumber", composite);
		createText(M.Formula, "formula", composite);
		createText(M.Synonyms, "synonyms", composite);
		createLocationViewer(composite);
	}

	private void createLocationViewer(Composite composite) {
		new Label(composite, SWT.NONE).setText(M.Location);
		LocationViewer viewer = new LocationViewer(composite);
		viewer.setNullable(true);
		viewer.setInput(Database.get());
		getBinding().onModel(() -> getModel(), "location", viewer);
	}
}
