package org.openlca.app.js;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.forms.editor.FormEditor;
import org.openlca.app.db.Database;
import org.openlca.app.util.Editors;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.model.Process;
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
			Bindings bindings = new SimpleBindings();
			bindings.put("log", LoggerFactory.getLogger(getClass()));
			bindings.put("db", Database.get());
			bindings.put("ProcessDao", ProcessDao.class);
			bindings.put("Process", Process.class);
			ScriptEngine engine = new ScriptEngineManager()
					.getEngineByName("nashorn");
			engine.setBindings(bindings, ScriptContext.GLOBAL_SCOPE);
			engine.eval(page.getScript());
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
