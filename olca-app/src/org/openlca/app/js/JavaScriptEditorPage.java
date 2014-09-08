package org.openlca.app.js;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.rcp.html.HtmlPage;
import org.openlca.app.rcp.html.HtmlView;
import org.openlca.app.util.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JavaScriptEditorPage extends FormPage implements HtmlPage {

	private Logger log = LoggerFactory.getLogger(getClass());
	private Browser browser;

	public JavaScriptEditorPage(JavaScriptEditor editor) {
		super(editor, "JavaScriptEditorPage", "JavaScript");
	}

	@Override
	public String getUrl() {
		return HtmlView.PYTHON_EDITOR.getUrl();
	}

	public String getScript() {
		try {
			Object script = browser.evaluate("return getContent();");
			if (script == null)
				return "";
			return script.toString();
		} catch (Exception e) {
			log.error("failed to get script content", e);
			return "";
		}
	}

	@Override
	public void onLoaded() {
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, "JavaScript Editor");
		FormToolkit toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		body.setLayout(new FillLayout());
		browser = UI.createBrowser(body, this);
	}

}
