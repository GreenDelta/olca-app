package org.openlca.app.editors.graphical.actions;

import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.edit.ExchangeEditPart;
import org.openlca.app.editors.graphical.edit.StickyNoteEditPart;
import org.openlca.app.rcp.images.Icon;

import static org.openlca.app.editors.graphical.requests.GraphRequestConstants.REQ_EDIT;

public class EditStickyNoteAction extends SelectionAction {

	public EditStickyNoteAction(GraphEditor part) {
		super(part);
		setId(ActionIds.EDIT_STICKY_NOTE);
		setText(M.Edit + " " + M.Note.toLowerCase());
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
		cc.setDebugLabel("Edit sticky note");

		var parts = getSelectedObjects();
		for (Object o : parts) {
			if (!(o instanceof StickyNoteEditPart noteEditPart))
				return null;

			cc.add(noteEditPart.getCommand(new Request(REQ_EDIT)));
		}
		return cc.unwrap();
	}

}