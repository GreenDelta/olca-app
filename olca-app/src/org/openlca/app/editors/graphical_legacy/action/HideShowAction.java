package org.openlca.app.editors.graphical_legacy.action;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.ui.parts.TreeViewer;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.StructuredSelection;
import org.openlca.app.M;
import org.openlca.app.editors.graphical_legacy.GraphEditor;
import org.openlca.app.editors.graphical_legacy.command.Commands;
import org.openlca.app.editors.graphical_legacy.command.HideShowCommand;
import org.openlca.app.editors.graphical_legacy.outline.ProcessTreeEditPart;
import org.openlca.core.model.descriptors.RootDescriptor;

class HideShowAction extends Action {

	final static int SHOW = 1;
	final static int HIDE = 2;

	private final GraphEditor editor;
	private final TreeViewer viewer;
	private final int type;

	HideShowAction(GraphEditor editor, TreeViewer viewer, int type) {
		if (type == SHOW) {
			setId(ActionIds.SHOW);
			setText(M.Show);
		} else if (type == HIDE) {
			setId(ActionIds.HIDE);
			setText(M.Hide);
		}
		this.editor = editor;
		this.viewer = viewer;
		this.type = type;
	}

	@Override
	public void run() {
		if (viewer.getSelection().isEmpty())
			return;
		Command command = null;
		for (Object o : ((StructuredSelection) viewer.getSelection()).toArray()) {
			if (!(o instanceof ProcessTreeEditPart part))
				continue;
			command = Commands.chain(createCommand(part.getModel()), command);
		}
		if (command == null)
			return;
		editor.getCommandStack().execute(command);
	}

	private Command createCommand(RootDescriptor process) {
		if (type == SHOW)
			return HideShowCommand.show(editor.getModel(), process);
		if (type == HIDE)
			return HideShowCommand.hide(editor.getModel(), process);
		return null;
	}

}
