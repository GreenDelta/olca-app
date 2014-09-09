package org.openlca.app.devtools.js;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.forms.editor.FormEditor;
import org.openlca.app.devtools.IScriptEditor;
import org.openlca.app.devtools.ScriptEditorInput;
import org.openlca.app.util.Editors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaScriptEditor extends FormEditor implements IScriptEditor {

	public static String ID = "JavaScriptEditor";
	private JavaScriptEditorPage page;

	public static void open() {
		Editors.open(new ScriptEditorInput("JavaScript"), ID);
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
		JavaScript.eval(page.getScript());
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

}
