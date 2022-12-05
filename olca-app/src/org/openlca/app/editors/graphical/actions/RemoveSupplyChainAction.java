package org.openlca.app.editors.graphical.actions;

import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.edit.ExchangeEditPart;
import org.openlca.app.editors.graphical.edit.NodeEditPart;
import org.openlca.app.editors.graphical.model.GraphLink;
import org.openlca.app.rcp.images.Icon;
import org.openlca.core.model.ProcessLink;

import java.util.*;

import static org.openlca.app.editors.graphical.requests.GraphRequestConstants.REQ_REMOVE_CHAIN;

public class RemoveSupplyChainAction extends SelectionAction {

	public static final String KEY_LINKS = "items";
	private final GraphEditor editor;

	public RemoveSupplyChainAction(GraphEditor part) {
		super(part);
		editor = part;
		setId(GraphActionIds.REMOVE_SUPPLY_CHAIN);
		setImageDescriptor(Icon.REMOVE_SUPPLY_CHAIN.descriptor());
	}

	@Override
	protected boolean calculateEnabled() {
		var command = getCommand();
		return command != null && command.canExecute();
	}

	@Override
	public void run() {
		execute(getCommand());
	}

	private Command getCommand() {
		if (getSelectedObjects().size() != 1)
			return null;

		var object = getSelectedObjects().get(0);
		NodeEditPart nodeEditPart;
		var links = new ArrayList<ProcessLink>();
		var linkSearch = editor.getModel().linkSearch;

		if (NodeEditPart.class.isAssignableFrom(object.getClass())) {
			setText(M.RemoveSupplyChain);
			nodeEditPart = (NodeEditPart) object;
			var nodeId = nodeEditPart.getModel().descriptor.id;
			links.addAll(linkSearch.getConnectionLinks(nodeId));
		}
		else if (object instanceof ExchangeEditPart part) {
			setText(M.RemoveFlowSupplyChain);
			nodeEditPart = (NodeEditPart) part.getParent().getParent();
			for (var connection : part.getModel().getTargetConnections())
				if (connection instanceof GraphLink link) {
					links.add(link.processLink);
				}
		} else return null;

		if (!links.isEmpty()) {
			var request = new Request(REQ_REMOVE_CHAIN);
			var data = new HashMap<String, Object>();
			data.put(KEY_LINKS, links);
			request.setExtendedData(data);
			return nodeEditPart.getCommand(request);
		} else return null;
	}

}
