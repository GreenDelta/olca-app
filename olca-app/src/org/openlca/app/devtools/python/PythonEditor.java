package org.openlca.app.devtools.python;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.forms.editor.FormEditor;
import org.openlca.app.App;
import org.openlca.app.db.Database;
import org.openlca.app.devtools.IScriptEditor;
import org.openlca.app.devtools.ScriptEditorInput;
import org.openlca.app.util.Editors;
import org.python.util.PythonInterpreter;
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
		try {
			// TODO: copy Lib with Python modules to workspace when used
			// the first time
			// set system properties on first use
			// PyScriptEngineFactory factory = new PyScriptEngineFactory();
			System.setProperty("python.path",
					"C:\\Users\\Besitzer\\Downloads\\xpy\\Lib");
			System.setProperty("python.home",
					"C:\\Users\\Besitzer\\Downloads\\xpy\\Lib");

			PythonInterpreter interpreter = new PythonInterpreter();
			interpreter.set("log", LoggerFactory.getLogger(getClass()));
			if (Database.get() != null)
				interpreter.set("db", Database.get());
			interpreter.set("app", App.class);
			interpreter.exec(page.getScript());
			interpreter.close();
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("failed to evaluate script", e);
		}
	}

}
