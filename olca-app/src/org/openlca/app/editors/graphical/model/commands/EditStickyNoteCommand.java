package org.openlca.app.editors.graphical.model.commands;

import org.eclipse.gef.commands.Command;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.StickyNote;
import org.openlca.app.util.UI;

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
		if (!EditStickyNoteDialog.open(newStickyNote))
			return;
		redo();
	}

	@Override
	public void redo() {
		parent.removeChild(oldStickyNote);
		parent.addChild(newStickyNote);
		parent.getEditor().setDirty();
	}

	@Override
	public void undo() {
		parent.removeChild(newStickyNote);
		parent.addChild(oldStickyNote);
		parent.getEditor().setDirty();
	}

}
