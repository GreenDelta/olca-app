package org.openlca.app.editors.graphical.edit;

import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.ComponentEditPolicy;
import org.eclipse.gef.requests.GroupRequest;
import org.openlca.app.editors.graphical.model.ExchangeItem;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.IOPane;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.app.editors.graphical.model.StickyNote;
import org.openlca.app.editors.graphical.model.commands.DeleteExchangeCommand;
import org.openlca.app.editors.graphical.model.commands.DeleteNodeCommand;
import org.openlca.app.editors.graphical.model.commands.DeleteStickyNoteCommand;
import org.openlca.app.editors.graphical.model.commands.EditExchangeCommand;
import org.openlca.app.editors.graphical.model.commands.EditStickyNoteCommand;
import org.openlca.app.editors.graphical.model.commands.SetReferenceCommand;

import static org.openlca.app.editors.graphical.requests.GraphRequestConstants.*;

public class GraphComponentEditPolicy extends ComponentEditPolicy {

	@Override
	public Command getCommand(Request request) {
		var child = getHost().getModel();
		if (REQ_EDIT.equals(request.getType())) {
			if (child instanceof ExchangeItem exchangeItem)
				return getEditExchangeCommand(exchangeItem);
			else if (child instanceof StickyNote note)
				return getEditStickyNoteCommand(note);
		}
		if (REQ_SET_REFERENCE.equals(request.getType())) {
			if (child instanceof ExchangeItem exchangeItem)
				return getSetReferenceCommand(exchangeItem);
		}
		return super.getCommand(request);
	}

	@Override
	protected Command createDeleteCommand(GroupRequest req) {
		var parent = getHost().getParent().getModel();
		var child = getHost().getModel();
		if (parent instanceof Graph graph && child instanceof Node node)
			return new DeleteNodeCommand(graph, node);
		if (parent instanceof Graph graph && child instanceof StickyNote note)
			return new DeleteStickyNoteCommand(graph, note);
		if (parent instanceof IOPane pane
			&& child instanceof ExchangeItem exchangeItem
			&& pane.getNode().isEditable())
			return new DeleteExchangeCommand(pane, exchangeItem);
		return super.createDeleteCommand(req);
	}

	protected Command getEditExchangeCommand(ExchangeItem exchangeItem) {
		return new EditExchangeCommand(exchangeItem);
	}

	protected Command getEditStickyNoteCommand(StickyNote note) {
		return new EditStickyNoteCommand(note);
	}

	protected Command getSetReferenceCommand(ExchangeItem exchangeItem) {
		return new SetReferenceCommand(exchangeItem);
	}


}
