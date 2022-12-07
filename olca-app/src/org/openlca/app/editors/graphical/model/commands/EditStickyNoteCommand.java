package org.openlca.app.editors.graphical.model.commands;

import org.eclipse.gef.commands.Command;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.StickyNote;

public class EditStickyNoteCommand extends Command {

	private final StickyNote oldStickyNote;
	private final Graph parent;
	private StickyNote newStickyNote;

	public EditStickyNoteCommand(StickyNote note) {
		this.oldStickyNote = note;
		this.parent = oldStickyNote.getGraph();
	}

	@Override
	public boolean canExecute() {
		return oldStickyNote != null;
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	@Override
	public void execute() {
		newStickyNote = oldStickyNote.copy();
		if (!StickyNoteDialog.open(newStickyNote))
			return;
		redo();
	}

	@Override
	public void redo() {
		parent.removeChild(oldStickyNote);
		parent.addChild(newStickyNote);
	}

	@Override
	public void undo() {
		parent.removeChild(newStickyNote);
		parent.addChild(oldStickyNote);
	}

}
