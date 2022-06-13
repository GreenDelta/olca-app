package org.openlca.app.editors.graphical.actions;

import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.ui.actions.StackAction;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.edit.NodeEditPart;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.app.rcp.images.Icon;

import static org.openlca.app.editors.graphical.model.commands.MinMaxCommand.MAXIMIZE;
import static org.openlca.app.editors.graphical.model.commands.MinMaxCommand.MINIMIZE;
import static org.openlca.app.editors.graphical.requests.GraphRequestConstants.REQ_MAX;
import static org.openlca.app.editors.graphical.requests.GraphRequestConstants.REQ_MIN;

public class MinMaxAllAction extends StackAction {

	private final int type;
	private final GraphEditor editor;

	public MinMaxAllAction(GraphEditor part, int type) {
		super(part);
		this.type = type;
		this.editor = part;
		if (type == MINIMIZE) {
			setId(ActionIds.MINIMIZE_ALL);
			setText(M.MinimizeAll);
			setImageDescriptor(Icon.MINIMIZE.descriptor());
		} else if (type == MAXIMIZE) {
			setId(ActionIds.MAXIMIZE_ALL);
			setText(M.MaximizeAll);
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
		var cc = new CompoundCommand();
		cc.setDebugLabel((type == MINIMIZE ? "Minimize" : "Maximize") + "all node");
		cc.setLabel(type == MINIMIZE
			? M.MinimizeAll.toLowerCase()
			: M.MaximizeAll.toLowerCase());

		for (Node node : editor.getModel().getChildren()) {
			var viewer = (GraphicalViewer) editor.getAdapter(GraphicalViewer.class);
			if (viewer == null)
				return null;
			var nodeEditPart = (NodeEditPart) viewer.getEditPartRegistry().get(node);
			if (nodeEditPart == null)
				return null;
			var request_type = type == MINIMIZE
				? REQ_MIN
				: REQ_MAX;
			cc.add(nodeEditPart.getCommand(new Request(request_type)));
		}

		return cc;
	}

}
