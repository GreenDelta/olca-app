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
import org.openlca.app.editors.graphical.model.GraphLink;
import org.openlca.app.editors.graphical.search.LinkSearchMap;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.MsgBox;
import org.openlca.core.model.ProcessLink;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.gef.RequestConstants.REQ_DELETE;

public class RemoveAllConnectionsAction extends SelectionAction {

	private final GraphEditor editor;

	public RemoveAllConnectionsAction(GraphEditor part) {
		super(part);
		editor = part;
		setId(GraphActionIds.REMOVE_ALL_CONNECTIONS);
		setText(M.RemoveConnections);
		setImageDescriptor(Icon.LINK.descriptor());
	}

	@Override
	protected boolean calculateEnabled() {
		if (getSelectedObjects().isEmpty())
			return false;

		for (Object o : getSelectedObjects()) {
			if (!(o instanceof NodeEditPart part))
				return false;
			if (part.getModel().getAllLinks().isEmpty())
				return false;
		}
		return true;
	}

	@Override
	public void run() {
		var command = getCommand();
		if (command != null) {
			if (command.canExecute())
				execute(getCommand());
			else MsgBox.info("Connections cannot be removed.");
		}
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
		List<GraphLink> links = new ArrayList<>();

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
			for (var l : node.getAllLinks()) {
				if (l instanceof GraphLink link) {
					if (!links.contains(link)) {
						links.add(link);
						var linkEditPart = (EditPart) viewer.getEditPartRegistry().get(link);
						cc.add(linkEditPart.getCommand(new GroupRequest(REQ_DELETE)));
					}
				}
			}
		}
		return cc;
	}

}
