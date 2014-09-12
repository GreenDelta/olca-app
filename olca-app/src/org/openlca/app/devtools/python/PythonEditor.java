package org.openlca.app.devtools.python;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.forms.editor.FormEditor;
import org.openlca.app.App;
import org.openlca.app.devtools.IScriptEditor;
import org.openlca.app.devtools.ScriptEditorInput;
import org.openlca.app.util.Editors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PythonEditor extends FormEditor implements IScriptEditor {

	public static String ID = "PythonEditor";

	private PythonEditorPage page;
	private Logger log = LoggerFactory.getLogger(getClass());

	public static void open() {
		Editors.open(new ScriptEditorInput("Python"), ID);
	}

	@Override
	protected void addPages() {
		try {
			page = new PythonEditorPage(this);
			addPage(page);
		} catch (Exception e) {
			log.error("failed to add editor page", e);
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void evalContent() {
		String script = page.getScript();
		App.run("Eval script", () -> Python.eval(script));
	}

}
