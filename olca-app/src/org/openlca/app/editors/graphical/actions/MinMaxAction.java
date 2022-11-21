package org.openlca.app.editors.graphical.actions;

import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.edit.NodeEditPart;
import org.openlca.app.rcp.images.Icon;


import static org.eclipse.gef.RequestConstants.REQ_OPEN;
import static org.openlca.app.editors.graphical.model.commands.MinMaxCommand.MAXIMIZE;
import static org.openlca.app.editors.graphical.model.commands.MinMaxCommand.MINIMIZE;

public class MinMaxAction extends SelectionAction {

	private final int type;
	private final GraphEditor editor;

	public MinMaxAction(GraphEditor part, int type) {
		super(part);
		this.type = type;
		this.editor = part;
		if (type == MINIMIZE) {
			setId(GraphActionIds.MINIMIZE);
			setText(M.Minimize);
			setImageDescriptor(Icon.MINIMIZE.descriptor());
		} else if (type == MAXIMIZE) {
			setId(GraphActionIds.MAXIMIZE);
			setText(M.Maximize);
			setImageDescriptor(Icon.MAXIMIZE.descriptor());
		}
	}

	@Override
	protected boolean calculateEnabled() {
		var command = getCommand();
		if (command == null)
			return false;
		return command.canExecute();
	}

	@Override
	public void run() {
		execute(getCommand());
	}

	private Command getCommand() {
		if (getSelectedObjects().isEmpty())
			return null;

		CompoundCommand cc = new CompoundCommand();
		cc.setDebugLabel((type == MINIMIZE ? "Minimize" : "Maximize"));

		var parts = getSelectedObjects();
		for (Object o : parts) {
			if (NodeEditPart.class.isAssignableFrom(o.getClass())) {
				var nodeEditPart = (NodeEditPart) o;
				if ((type == MINIMIZE ^ nodeEditPart.getModel().isMinimized()))
					cc.add(nodeEditPart.getCommand(new Request(REQ_OPEN)));
			}
		}
		return cc.unwrap();
	}

}
