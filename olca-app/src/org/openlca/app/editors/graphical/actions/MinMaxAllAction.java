package org.openlca.app.editors.graphical.actions;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.ui.actions.StackAction;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.model.MinMaxComponent;
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
			setId(GraphActionIds.MINIMIZE_ALL);
			setText(M.MinimizeAll);
			setImageDescriptor(Icon.MINIMIZE.descriptor());
		} else if (type == MAXIMIZE) {
			setId(GraphActionIds.MAXIMIZE_ALL);
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
		editor.updateStackActions();
	}

	private Command getCommand() {
		var cc = new CompoundCommand();
		cc.setDebugLabel((type == MINIMIZE ? "Minimize" : "Maximize") + "all node");
		cc.setLabel(type == MINIMIZE
			? M.MinimizeAll.toLowerCase()
			: M.MaximizeAll.toLowerCase());

		for (MinMaxComponent component : editor.getModel().getMinMaxComponents()) {
			var viewer = (GraphicalViewer) editor.getAdapter(GraphicalViewer.class);
			if (viewer == null)
				return null;
			var registry = viewer.getEditPartRegistry();
			var componentEditPart = (EditPart) registry.get(component);
			if (componentEditPart == null)
				return null;
			var request_type = type == MINIMIZE
				? REQ_MIN
				: REQ_MAX;
			cc.add(componentEditPart.getCommand(new Request(request_type)));
		}

		return cc.unwrap();
	}

}
