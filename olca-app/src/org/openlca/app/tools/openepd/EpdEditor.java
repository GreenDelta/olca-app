package org.openlca.app.tools.openepd;

import java.util.UUID;

import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.UI;

public class EpdEditor extends SimpleFormEditor {

	public static void open() {
		var id = UUID.randomUUID().toString();
		Editors.open(new SimpleEditorInput(id, "New EPD"), "EpdEditor");
	}

	@Override
	protected FormPage getPage() {
		return new Page(this);
	}

	private static class Page extends FormPage {

		public Page(EpdEditor editor) {
			super(editor, "EpdEditor.Page", "New EPD");
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			var form = UI.formHeader(mform, "Building Transparency - openEPD",
				Icon.EC3_WIZARD.get());
			var tk = mform.getToolkit();
			var body = UI.formBody(form, tk);
			var loginPanel = LoginPanel.create(body, tk);
		}

	}

}
