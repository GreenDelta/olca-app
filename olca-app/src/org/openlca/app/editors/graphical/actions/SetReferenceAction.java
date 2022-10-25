package org.openlca.app.editors.graphical.actions;

import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.osgi.util.NLS;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.edit.ExchangeEditPart;
import org.openlca.app.rcp.images.Icon;

import static org.openlca.app.editors.graphical.requests.GraphRequestConstants.REQ_SET_REFERENCE;

public class SetReferenceAction extends SelectionAction {

	private ExchangeEditPart part;

	public SetReferenceAction(GraphEditor part) {
		super(part);
		setId(GraphActionIds.SET_REFERENCE);
		setText(NLS.bind(M.SetAsQuantitativeReference, M.Reference));
		setImageDescriptor(Icon.FORMULA.descriptor());
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
		cc.setDebugLabel("Set node as quantitative reference.");

		var parts = getSelectedObjects();
		for (Object o : parts) {
			if (!(o instanceof ExchangeEditPart exchangeEditPart))
				return null;
			part = exchangeEditPart;
			cc.add(exchangeEditPart.getCommand(new Request(REQ_SET_REFERENCE)));
		}
		return cc.unwrap();
	}
}
