package org.openlca.app.devtools.sql;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.forms.editor.FormEditor;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.devtools.ScriptEditorInput;
import org.openlca.app.util.Editors;
import org.openlca.app.util.Info;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlEditor extends FormEditor {

	public static String ID = "SqlEditor";

	public static void open() {
		if (Database.get() == null) {
			Info.showBox(Messages.NoDatabaseOpened,
					Messages.NeedOpenDatabase);
			return;
		}
		Editors.open(new ScriptEditorInput("SQL"), ID);
	}

	@Override
	protected void addPages() {
		try {
			addPage(new SqlEditorPage(this));
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("failed to open Sql Editor page", e);
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

}
