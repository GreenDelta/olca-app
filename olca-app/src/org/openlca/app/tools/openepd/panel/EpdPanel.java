package org.openlca.app.tools.openepd.panel;

import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.util.UI;

public class EpdPanel extends SimpleFormEditor {

	public static void open() {
		Editors.open(new SimpleEditorInput("EpdPanel", "openEPD"), "EpdPanel");
	}

	@Override
	protected FormPage getPage() {
		return new Page(this);
	}

	private static class Page extends FormPage {

		public Page(EpdPanel panel) {
			super(panel, "EpdPanel.Page", "openEPD");
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			var form = UI.formHeader(mform, "Flow mapping");
			var tk = mform.getToolkit();
			var body = UI.formBody(form, tk);
			CredentialsSection.create(body, tk);

			form.reflow(true);
		}
	}
}
