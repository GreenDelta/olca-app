package org.openlca.app.editors.epds;

import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.UI;
import org.openlca.core.model.Epd;

class InfoPage extends ModelPage<Epd> {

	private final EpdEditor editor;

	InfoPage(EpdEditor editor) {
		super(editor, "EpdInfoPage", M.GeneralInformation);
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var form = UI.header(this);
		var tk = mForm.getToolkit();
		var body = UI.body(form, tk);

		var info = new InfoSection(editor).render(body, tk);
		UI.filler(info.composite());
		new UploadButton(editor).render(info.composite(), tk);

		new EpdProductSection(editor).render(body, tk);
		new EpdModulesSection(editor).render(body, tk);
		body.setFocus();
		form.reflow(true);
	}
}
