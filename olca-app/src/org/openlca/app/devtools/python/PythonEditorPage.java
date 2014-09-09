package org.openlca.app.devtools.python;

import org.openlca.app.devtools.ScriptEditorPage;
import org.openlca.app.rcp.html.HtmlView;

class PythonEditorPage extends ScriptEditorPage {

	public PythonEditorPage(PythonEditor editor) {
		super(editor, "PythonEditorPage", "Python");
	}

	@Override
	public String getUrl() {
		return HtmlView.PYTHON_EDITOR.getUrl();
	}

}
