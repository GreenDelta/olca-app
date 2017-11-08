package org.openlca.app.editors.actors;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.M;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.UI;
import org.openlca.core.model.Actor;

class ActorInfoPage extends ModelPage<Actor> {

	private FormToolkit toolkit;
	private ScrolledForm form;

	ActorInfoPage(ActorEditor editor) {
		super(editor, "ActorInfoPage", M.GeneralInformation);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		form = UI.formHeader(managedForm);
		updateFormTitle();
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		InfoSection infoSection = new InfoSection(getEditor());
		infoSection.render(body, toolkit);
		createAdditionalInfo(body);
		body.setFocus();
		form.reflow(true);
	}

	@Override
	protected void updateFormTitle() {
		if (form == null)
			return;
		form.setText(M.Actor + ": " + getModel().getName());
	}

	private void createAdditionalInfo(Composite body) {
		Composite composite = UI.formSection(body, toolkit, M.AdditionalInformation, 3);
		text(composite, M.Address, "address");
		text(composite, M.City, "city");
		text(composite, M.Country, "country");
		text(composite, M.Email, "email");
		text(composite, M.Telefax, "telefax");
		text(composite, M.Telephone, "telephone");
		text(composite, M.Website, "website");
		text(composite, M.ZipCode, "zipCode");
	}

}
