package org.openlca.app.editors.actors;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.UI;
import org.openlca.core.model.Actor;

class ActorPage extends ModelPage<Actor> {

	ActorPage(ActorEditor editor) {
		super(editor, "ActorInfoPage", M.GeneralInformation);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var form = UI.formHeader(this);
		var tk = mform.getToolkit();
		var body = UI.formBody(form, tk);
		var infoSection = new InfoSection(getEditor());
		infoSection.render(body, tk);
		createAdditionalInfo(body, tk);
		body.setFocus();
		form.reflow(true);
	}

	private void createAdditionalInfo(Composite body, FormToolkit tk) {
		var composite = UI.formSection(body, tk, M.AdditionalInformation, 3);
		text(composite, M.Address, "address");
		text(composite, M.City, "city");
		text(composite, M.ZipCode, "zipCode");
		text(composite, M.Country, "country");
		text(composite, M.Email, "email");
		text(composite, M.Telefax, "telefax");
		text(composite, M.Telephone, "telephone");
		text(composite, M.Website, "website");
	}

}
