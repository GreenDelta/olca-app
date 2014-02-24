package org.openlca.app.db.sql;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.forms.editor.FormEditor;
import org.openlca.app.db.Database;
import org.openlca.app.util.Editors;
import org.openlca.app.util.Info;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlEditor extends FormEditor {

	public static String ID = "SqlEditor";

	public static void open() {
		if (Database.get() == null) {
			Info.showBox("No database connection",
					"You first need to activate a database.");
			return;
		}
		Editors.open(new EditorInput(), ID);
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
			return "SQL Editor";
		}

		@Override
		public IPersistableElement getPersistable() {
			return null;
		}

		@Override
		public String getToolTipText() {
			return "SQL Editor";
		}

		@Override
		@SuppressWarnings("rawtypes")
		public Object getAdapter(Class aClass) {
			return null;
		}
	}
}
