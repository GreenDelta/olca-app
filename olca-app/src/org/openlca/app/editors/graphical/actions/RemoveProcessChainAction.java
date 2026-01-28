package org.openlca.app.editors.graphical.actions;

import static org.openlca.app.editors.graphical.requests.GraphRequestConstants.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.edit.ExchangeEditPart;
import org.openlca.app.editors.graphical.edit.NodeEditPart;
import org.openlca.app.editors.graphical.model.GraphLink;
import org.openlca.app.rcp.images.Icon;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ProcessLink;

public class RemoveProcessChainAction extends SelectionAction {

	public static final String KEY_LINKS = "links";
	private final GraphEditor editor;

	public RemoveProcessChainAction(GraphEditor editor) {
		super(editor);
		this.editor = editor;
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
		if (getSelectedObjects().isEmpty())
			return null;

		var viewer = getWorkbenchPart().getAdapter(GraphicalViewer.class);
		var registry = viewer.getEditPartRegistry();
		var graphPart = registry.get(editor.getModel());
		if (graphPart == null)
			return null;

		var links = new ArrayList<ProcessLink>();


		for (var object : getSelectedObjects()) {
			addContributor(object, links);
		}

		if (!links.isEmpty()) {
			var request = new Request(REQ_REMOVE_CHAIN);
			var data = new HashMap<String, Object>();
			data.put(KEY_LINKS, links);
			request.setExtendedData(data);
			return graphPart.getCommand(request);
		} else return null;
	}

	private void addContributor(Object object, List<ProcessLink> links) {
		var linkSearch = editor.getModel().linkSearch;
		if (NodeEditPart.class.isAssignableFrom(object.getClass())) {
			setText(M.RemoveSupplyChain);
			var nodeId = ((NodeEditPart) object).getModel().descriptor.id;
			links.addAll(linkSearch.getConsumerLinks(nodeId));
		} else if (object instanceof ExchangeEditPart part) {
			setText(M.RemoveFlowSupplyChain);
			var e = part.getModel().exchange;
			if ((e.flow.flowType == FlowType.WASTE_FLOW && !e.isInput)
				|| (e.flow.flowType == FlowType.PRODUCT_FLOW && e.isInput)) {
				var connection = part.getModel().getAllConnections();
				// there should only one link to a waste output or product input.
				if (connection.size() != 1)
					return;
				if (connection.getFirst() instanceof GraphLink link)
					links.add(link.processLink);
			}
		}
	}


}
