package org.openlca.app.editors.locations;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.M;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.UI;
import org.openlca.core.model.Location;

class LocationInfoPage extends ModelPage<Location> {

	private final LocationEditor editor;

	LocationInfoPage(LocationEditor editor) {
		super(editor, "LocationInfoPage", M.GeneralInformation);
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		ScrolledForm form = UI.formHeader(this);
		FormToolkit tk = mform.getToolkit();
		Composite body = UI.formBody(form, tk);
		InfoSection infoSection = new InfoSection(getEditor());
		infoSection.render(body, tk);
		createAdditionalInfo(body, tk);
		new MapSection(editor).render(body, tk);
		body.setFocus();
		form.reflow(true);
	}

	private void createAdditionalInfo(Composite body, FormToolkit tk) {
		Composite comp = UI.formSection(body, tk, M.AdditionalInformation, 3);
		text(comp, M.Code, "code");
		doubleText(comp, M.Longitude, "longitude");
		doubleText(comp, M.Latitude, "latitude");
	}

}
