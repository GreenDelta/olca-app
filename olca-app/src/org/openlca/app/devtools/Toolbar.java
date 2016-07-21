package org.openlca.app.devtools;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Editors;
import org.openlca.app.util.Info;

public class Toolbar extends EditorActionBarContributor {

	@Override
	public void contributeToToolBar(IToolBarManager manager) {
		manager.add(Actions.create("Run", Icon.RUN.descriptor(),
				() -> {
					if (Database.get() == null) {
						Info.showBox(M.NoDatabaseOpened, M.NeedOpenDatabase);
						return;
					}
					FormEditor editor = Editors.getActive();
					if (!(editor instanceof IScriptEditor))
						return;
					IScriptEditor scriptEditor = (IScriptEditor) editor;
					scriptEditor.evalContent();
				}));
	}
}
