package org.openlca.app.editors.graphical.action;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.ui.parts.TreeViewer;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.StructuredSelection;
import org.openlca.app.Messages;
import org.openlca.app.editors.graphical.ProductSystemGraphEditor;
import org.openlca.app.editors.graphical.command.CommandFactory;
import org.openlca.app.editors.graphical.outline.ProcessTreeEditPart;
import org.openlca.core.model.descriptors.ProcessDescriptor;

class HideShowAction extends Action {

	final static int SHOW = 1;
	final static int HIDE = 2;

	private ProductSystemGraphEditor editor;
	private TreeViewer viewer;
	private int type;

	HideShowAction(int type) {
		if (type == SHOW) {
			setId(ActionIds.SHOW);
			setText(Messages.Show);
		} else if (type == HIDE) {
			setId(ActionIds.HIDE);
			setText(Messages.Hide);
		}
		this.type = type;
	}

	@Override
	public void run() {
		if (viewer.getSelection().isEmpty())
			return;
		Command command = null;
		for (Object o : ((StructuredSelection) viewer.getSelection()).toArray()) {
			if (o instanceof ProcessTreeEditPart) {
				ProcessTreeEditPart part = (ProcessTreeEditPart) o;
				if (command == null)
					command = createCommand(part.getModel());
				else
					command = command.chain(createCommand(part.getModel()));
			}
		}
		if (command != null)
			editor.getCommandStack().execute(command);
	}

	private Command createCommand(ProcessDescriptor process) {
		if (type == SHOW)
			return CommandFactory.createShowCommand(process, editor.getModel());
		if (type == HIDE)
			return CommandFactory.createHideCommand(process, editor.getModel());
		return null;
	}
 
	void setEditor(ProductSystemGraphEditor editor) {
		this.editor = editor;
	}

	void setViewer(TreeViewer viewer) {
		this.viewer = viewer;
	}

}
