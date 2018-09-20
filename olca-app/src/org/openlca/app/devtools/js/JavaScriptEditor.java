package org.openlca.app.devtools.js;

import java.util.UUID;

import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.devtools.IScriptEditor;
import org.openlca.app.devtools.ScriptEditorPage;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.rcp.html.HtmlView;
import org.openlca.app.rcp.images.Icon;

public class JavaScriptEditor extends SimpleFormEditor implements IScriptEditor {

	public static String TYPE = "JavaScriptEditor";
	private Page page;

	public static void open() {
		Editors.open(new SimpleEditorInput(TYPE, UUID.randomUUID().toString(), "JavaScript"), TYPE);
	}

	@Override
	protected FormPage getPage() {
		return page = new Page();
	}

	public void evalContent() {
		JavaScript.eval(page.getScript());
	}

	private class Page extends ScriptEditorPage {

		public Page() {
			super(JavaScriptEditor.this, "JavaScriptEditorPage", "JavaScript", Icon.JAVASCRIPT.get());
		}

		@Override
		public String getUrl() {
			return HtmlView.JAVASCRIPT_EDITOR.getUrl();
		}

	}
}
