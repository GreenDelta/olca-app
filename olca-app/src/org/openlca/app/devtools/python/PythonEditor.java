package org.openlca.app.devtools.python;

import java.util.UUID;

import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.App;
import org.openlca.app.devtools.IScriptEditor;
import org.openlca.app.devtools.ScriptEditorPage;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.rcp.html.HtmlView;
import org.openlca.app.rcp.images.Icon;

public class PythonEditor extends SimpleFormEditor implements IScriptEditor {

	public static String TYPE = "PythonEditor";
	private Page page;

	public static void open() {
		Editors.open(new SimpleEditorInput(TYPE, UUID.randomUUID().toString(), "Python"), TYPE);
	}

	@Override
	protected FormPage getPage() {
		return page = new Page();
	}

	@Override
	public void evalContent() {
		String script = page.getScript();
		App.run("Eval script", () -> Python.eval(script));
	}

	private class Page extends ScriptEditorPage {

		public Page() {
			super(PythonEditor.this, "PythonEditorPage", "Python", Icon.PYTHON.get());
		}

		@Override
		public String getUrl() {
			return HtmlView.PYTHON_EDITOR.getUrl();
		}

	}
}
