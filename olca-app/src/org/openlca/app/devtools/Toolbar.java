package org.openlca.app.devtools;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.openlca.app.editors.Editors;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Actions;

public class Toolbar extends EditorActionBarContributor {

	@Override
	public void contributeToToolBar(IToolBarManager manager) {
		manager.add(Actions.create("Run", Icon.RUN.descriptor(), () -> {
			var active = Editors.getActive();
			if (!(active instanceof ScriptingEditor))
				return;
			var editor = (ScriptingEditor) active;
			editor.eval();
		}));
	}
}
