package org.openlca.app.editors.actors;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.Messages;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.UI;
import org.openlca.core.model.Actor;

class ActorInfoPage extends ModelPage<Actor> {

	private FormToolkit toolkit;

	ActorInfoPage(ActorEditor editor) {
		super(editor, "ActorInfoPage", Messages.GeneralInformation);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, Messages.Actor + ": "
				+ getModel().getName());
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		InfoSection infoSection = new InfoSection(getEditor());
		infoSection.render(body, toolkit);
		createAdditionalInfo(body);
		body.setFocus();
		form.reflow(true);
	}

	private void createAdditionalInfo(Composite body) {
		Composite composite = UI.formSection(body, toolkit,
				Messages.AdditionalInformation);
		createText(Messages.Address, "address", composite);
		createText(Messages.City, "city", composite);
		createText(Messages.Country, "country", composite);
		createText(Messages.Email, "email", composite);
		createText(Messages.Telefax, "telefax", composite);
		createText(Messages.Telephone, "telephone", composite);
		createText(Messages.Website, "website", composite);
		createText(Messages.ZipCode, "zipCode", composite);
	}

}
