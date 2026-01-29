package org.openlca.app.editors.graphical.edit;

import static org.openlca.app.editors.graphical.actions.RemoveChainAction.*;
import static org.openlca.app.editors.graphical.requests.GraphRequests.*;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.RootComponentEditPolicy;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.commands.RemoveSupplyChainCommand;
import org.openlca.core.model.ProcessLink;

public class GraphEditPolicy extends RootComponentEditPolicy {

	@Override
	public Command getCommand(Request request) {
		return REQ_REMOVE_CHAIN.equals(request.getType())
			? getRemoveSupplyChainCommand(request)
			: super.getCommand(request);
	}

	private Command getRemoveSupplyChainCommand(Request req) {
		var object = req.getExtendedData().get(KEY_LINKS);
		if (object instanceof Collection<?> collection) {
			var links = new ArrayList<ProcessLink>();
			for (var obj : collection)
				links.add((ProcessLink) obj);
			return new RemoveSupplyChainCommand(links, (Graph) getHost().getModel());
		}
		else return null;
	}

}
