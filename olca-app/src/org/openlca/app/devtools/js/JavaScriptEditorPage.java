package org.openlca.app.devtools.js;

import org.openlca.app.devtools.ScriptEditorPage;
import org.openlca.app.rcp.html.HtmlView;

class JavaScriptEditorPage extends ScriptEditorPage {

	public JavaScriptEditorPage(JavaScriptEditor editor) {
		super(editor, "JavaScriptEditorPage", "JavaScript");
	}

	@Override
	public String getUrl() {
		return HtmlView.JAVASCRIPT_EDITOR.getUrl();
	}

}
