package org.openlca.app.js;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.util.UI;

class JavaScriptEditorPage extends FormPage {

	private StyledText scriptText;

	public JavaScriptEditorPage(JavaScriptEditor editor) {
		super(editor, "JavaScriptEditorPage", "JavaScript");
	}

	public String getScript() {
		return scriptText.getText();
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, "JavaScript Editor");
		FormToolkit toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		body.setLayout(new FillLayout());
		scriptText = new StyledText(body, SWT.BORDER);
		toolkit.adapt(scriptText);
	}
}
