package org.openlca.app.editors.graphical.edit;

import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.RootComponentEditPolicy;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.commands.RemoveSupplyChainCommand;
import org.openlca.core.model.ProcessLink;

import java.util.ArrayList;
import java.util.Collection;

import static org.openlca.app.editors.graphical.actions.RemoveSupplyChainAction.KEY_LINKS;
import static org.openlca.app.editors.graphical.requests.GraphRequestConstants.REQ_REMOVE_CHAIN;

public class GraphEditPolicy extends RootComponentEditPolicy {

	@Override
	public Command getCommand(Request request) {
		if (REQ_REMOVE_CHAIN.equals(request.getType()))
			return getRemoveSupplyChainCommand(request);
		return super.getCommand(request);
	}

	private Command getRemoveSupplyChainCommand(Request request) {
		var object = request.getExtendedData().get(KEY_LINKS);
		if (object instanceof Collection<?> collection) {
			var links = new ArrayList<ProcessLink>();
			for (var obj : collection)
				links.add((ProcessLink) obj);
			return new RemoveSupplyChainCommand(links, (Graph) getHost().getModel());
		}
		else return null;
	}

}
