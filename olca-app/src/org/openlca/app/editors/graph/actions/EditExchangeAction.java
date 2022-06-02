package org.openlca.app.editors.graph.actions;

import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.osgi.util.NLS;
import org.openlca.app.M;
import org.openlca.app.editors.graph.GraphEditor;
import org.openlca.app.editors.graph.edit.ExchangeEditPart;
import org.openlca.app.rcp.images.Icon;

import static org.openlca.app.editors.graph.requests.GraphRequestConstants.REQ_EDIT;

public class EditExchangeAction extends SelectionAction {

	public EditExchangeAction(GraphEditor part) {
		super(part);
		setId(ActionIds.EDIT_EXCHANGE);
		// TODO (francois) NLS.bind does not seem to work.
		//		setText(NLS.bind(M.Edit, M.Flow));
		setText("Edit flow");
		setImageDescriptor(Icon.EDIT.descriptor());
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

		CompoundCommand cc = new CompoundCommand();
		cc.setDebugLabel("Add exchange item");

		var parts = getSelectedObjects();
		for (Object o : parts) {
			if (!(o instanceof ExchangeEditPart exchangeEditPart))
				return null;

			cc.add(exchangeEditPart.getCommand(new Request(REQ_EDIT)));
		}
		return cc.unwrap();
	}

}
