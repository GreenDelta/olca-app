package org.openlca.app.editors.graphical.actions;

import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.osgi.util.NLS;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.edit.ExchangeEditPart;

import static org.openlca.app.editors.graphical.requests.GraphRequestConstants.REQ_SET_REFERENCE;

public class SetReferenceAction extends SelectionAction {

	public SetReferenceAction(GraphEditor part) {
		super(part);
		setId(GraphActionIds.SET_REFERENCE);
		setText(NLS.bind(M.SetAsQuantitativeReference, M.Reference));
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
		if (getSelectedObjects().size() != 1)
			return null;

		var part = getSelectedObjects().get(0);
		if (!(part instanceof ExchangeEditPart exchangeEditPart))
			return null;
		else return exchangeEditPart.getCommand(new Request(REQ_SET_REFERENCE));
	}
	
}
