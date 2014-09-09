package org.openlca.app.devtools;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Editors;

public class Toolbar extends EditorActionBarContributor {

	@Override
	public void contributeToToolBar(IToolBarManager manager) {
		manager.add(Actions.create("Run", ImageType.RUN_SMALL.getDescriptor(),
				() -> {
					FormEditor editor = Editors.getActive();
					if (!(editor instanceof IScriptEditor))
						return;
					IScriptEditor scriptEditor = (IScriptEditor) editor;
					scriptEditor.evalContent();
				}));
	}
}
