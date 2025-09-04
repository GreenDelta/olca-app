package org.openlca.app.devtools;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.openlca.app.M;
import org.openlca.app.devtools.agent.AgentEditor;
import org.openlca.app.editors.Editors;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Actions;

public class Toolbar extends EditorActionBarContributor {

	@Override
	public void contributeToToolBar(IToolBarManager manager) {
		manager.add(Actions.create(M.Run, Icon.RUN.descriptor(), () -> {
			var active = Editors.getActive();
			if (!(active instanceof ScriptingEditor editor))
				return;
			editor.eval();
		}));
		manager.add(Actions.create("Agent", Icon.PYTHON.descriptor(), AgentEditor::open));
	}
}
