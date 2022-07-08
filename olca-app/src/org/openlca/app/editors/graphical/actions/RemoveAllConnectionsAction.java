package org.openlca.app.editors.graphical.actions;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.requests.GroupRequest;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.edit.NodeEditPart;
import org.openlca.app.editors.graphical.model.Link;
import org.openlca.app.editors.graphical.search.LinkSearchMap;
import org.openlca.core.model.ProcessLink;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.gef.RequestConstants.REQ_DELETE;

public class RemoveAllConnectionsAction extends SelectionAction {

	private final GraphEditor editor;

	public RemoveAllConnectionsAction(GraphEditor part) {
		super(part);
		editor = part;
		setId(ActionIds.REMOVE_ALL_CONNECTIONS);
		setText(M.RemoveConnections);
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

		var viewer = (GraphicalViewer) editor.getAdapter(GraphicalViewer.class);
		if (viewer == null)
			return null;

		CompoundCommand cc = new CompoundCommand();
		cc.setDebugLabel("Remove links");
		cc.setLabel(M.RemoveConnections.toLowerCase());

		var parts = getSelectedObjects();
		List<Link> links = new ArrayList<>();

		for (Object o : parts) {
			if (!(o instanceof NodeEditPart nodeEditPart))
				return null;

			var node = nodeEditPart.getModel();

			// create new link search to avoid problems with missing entries before
			// ConnectionLink.unlink is called
			List<ProcessLink> pLinks = editor.getProductSystem().processLinks;
			var linkSearch = new LinkSearchMap(pLinks);
			List<ProcessLink> processLinks = linkSearch.getLinks(node.descriptor.id);
			for (ProcessLink link : processLinks)
				linkSearch.remove(link);
			for (var link : node.getAllLinks()) {
				if (!links.contains(link)) {
					links.add(link);
					var linkEditPart = (EditPart) viewer.getEditPartRegistry().get(link);
					cc.add(linkEditPart.getCommand(new GroupRequest(REQ_DELETE)));
				}
			}
		}
		return cc;
	}

}
