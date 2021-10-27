package org.openlca.app.editors.parameters.bigtable;

import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.util.MsgBox;

/**
 * This is a simple editor that contains a table with all parameters of the
 * database (global and local).
 */
public class BigParameterTable extends SimpleFormEditor {

	public static void show() {
		if (Database.get() == null) {
			MsgBox.info(M.NoDatabaseOpened, M.NeedOpenDatabase);
			return;
		}
		String id = "BigParameterTable";
		Editors.open(new SimpleEditorInput(id, M.Parameters), id);
	}

	@Override
	protected FormPage getPage() {
		return new EditorPage(this);
	}
}
