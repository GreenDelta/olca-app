package org.openlca.app.devtools.csharp;

import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.App;
import org.openlca.app.devtools.IScriptEditor;
import org.openlca.app.devtools.ScriptEditorPage;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.rcp.html.HtmlView;
import org.openlca.app.util.DefaultInput;
import org.openlca.app.util.Editors;

public class CSharpEditor extends SimpleFormEditor implements IScriptEditor {

	public static String ID = "CSharpEditor";
	private Page page;

	public static void open() {
		Editors.open(new DefaultInput(ID, "C#"), ID);
	}

	@Override
	protected FormPage getPage() {
		return page = new Page();
	}

	@Override
	public void evalContent() {
		String script = page.getScript();
		App.run("Eval script", () -> CSharp.eval(script));
	}

	private class Page extends ScriptEditorPage {

		public Page() {
			super(CSharpEditor.this, "CSharpEditorPage", "C#");
		}

		@Override
		public String getUrl() {
			return HtmlView.CSHARP_EDITOR.getUrl();
		}

	}
}
