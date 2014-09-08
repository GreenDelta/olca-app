package org.openlca.app.js;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.forms.editor.FormEditor;
import org.openlca.app.App;
import org.openlca.app.db.Database;
import org.openlca.app.util.Editors;
import org.python.util.PythonInterpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaScriptEditor extends FormEditor {

	public static String ID = "JavaScriptEditor";
	private JavaScriptEditorPage page;

	public static void open() {
		Editors.open(new EditorInput(), ID);
	}

	@Override
	protected void addPages() {
		try {
			page = new JavaScriptEditorPage(this);
			addPage(page);
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("failed to open Sql Editor page", e);
		}
	}

	public void evalContent() {
		try {
			// Bindings bindings = new SimpleBindings();
			// bindings.put("log", LoggerFactory.getLogger(getClass()));
			// bindings.put("db", Database.get());
			// bindings.put("ProcessDao", ProcessDao.class);
			// bindings.put("Process", Process.class);
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

			// interpreter.exec("import sys");

			interpreter.exec(page.getScript());

			interpreter.close();
			// ScriptEngine engine = factory.getScriptEngine();
			// // engine.setBindings(new ScriptEngineManager().getBindings(),
			// // ScriptContext.GLOBAL_SCOPE);
			// engine.setBindings(bindings, ScriptContext.GLOBAL_SCOPE);
			// engine.eval(page.getScript());
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("failed to evaluate script", e);
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

	private static class EditorInput implements IEditorInput {

		@Override
		public boolean exists() {
			return true;
		}

		@Override
		public ImageDescriptor getImageDescriptor() {
			return null;
		}

		@Override
		public String getName() {
			return "JavaScript Editor";
		}

		@Override
		public IPersistableElement getPersistable() {
			return null;
		}

		@Override
		public String getToolTipText() {
			return "JavaScript Editor";
		}

		@Override
		@SuppressWarnings("rawtypes")
		public Object getAdapter(Class aClass) {
			return null;
		}
	}
}
