package org.openlca.app.editors.epds;

import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.UI;
import org.openlca.core.model.Epd;

public class EpdEditor extends ModelEditor<Epd> {

	public EpdEditor() {
		super(Epd.class);
	}

	@Override
	protected void addPages() {
		try {
			addPage(new EpdPage(this));
		} catch (Exception e) {
			ErrorReporter.on("Failed to open EPD", e);
		}
	}

	private static class EpdPage extends ModelPage<Epd> {

		EpdPage(EpdEditor editor) {
			super(editor, "EpdInfoPage", M.GeneralInformation);
		}

		@Override
		protected void createFormContent(IManagedForm mForm) {
			var form = UI.formHeader(this);
			var tk = mForm.getToolkit();
			var body = UI.formBody(form, tk);
			new InfoSection(getEditor()).render(body, tk);
			body.setFocus();
			form.reflow(true);
		}
	}
}
