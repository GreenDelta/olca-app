package org.openlca.app.tools.graphics.actions;

import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.openlca.app.M;
import org.openlca.app.tools.graphics.frame.GraphicalEditorWithFrame;
import org.openlca.app.rcp.images.Icon;

public class MinimapAction extends WorkbenchPartAction {

	private final GraphicalEditorWithFrame editor;

	public MinimapAction(GraphicalEditorWithFrame editor) {
		super(editor);
		this.editor = editor;
	}

	@Override
	protected void init() {
		setId(ActionIds.MINIMAP);
		setText(M.Minimap);
		setImageDescriptor(Icon.MINIMAP.descriptor());
		setChecked(true);
	}

	@Override
	public void run() {
		var minimap = editor.getMinimap();
		minimap.setVisible(!minimap.isVisible());
		setChecked(minimap.isVisible());
	}

	@Override
	protected boolean calculateEnabled() {
		return editor != null
				&& editor.getRootEditPart() != null;
	}

}
