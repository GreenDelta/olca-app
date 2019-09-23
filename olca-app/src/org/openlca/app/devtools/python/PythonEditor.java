package org.openlca.app.devtools.python;

import java.util.UUID;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.devtools.IScriptEditor;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.rcp.html.HtmlFolder;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PythonEditor extends SimpleFormEditor implements IScriptEditor {

	public static String TYPE = "PythonEditor";
	private Page page;

	public static void open() {
		Editors.open(new SimpleEditorInput(
				TYPE, UUID.randomUUID().toString(), "Python"), TYPE);
	}

	@Override
	protected FormPage getPage() {
		return page = new Page();
	}

	@Override
	public void evalContent() {
		String script = page.getScript();
		App.run("Eval script", () -> Python.exec(script));
	}

	private class Page extends FormPage {

		private Browser browser;

		public Page() {
			super(PythonEditor.this, "PythonEditorPage", "Python");
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			ScrolledForm form = UI.formHeader(
					mform, getTitle(), Icon.PYTHON.get());
			FormToolkit toolkit = mform.getToolkit();
			Composite body = UI.formBody(form, toolkit);
			body.setLayout(new FillLayout());
			try {
				browser = new Browser(body, SWT.NONE);
				browser.setJavascriptEnabled(true);
				browser.setUrl(HtmlFolder.getUrl("python.html"));
			} catch (Exception e) {
				Logger log = LoggerFactory.getLogger(getClass());
				log.error("failed to create browser in Python editor", e);
			}
		}

		public String getScript() {
			try {
				Object script = browser.evaluate("return getContent();");
				if (script == null)
					return "";
				return script.toString();
			} catch (Exception e) {
				Logger log = LoggerFactory.getLogger(getClass());
				log.error("failed to get script content", e);
				return "";
			}
		}
	}
}
