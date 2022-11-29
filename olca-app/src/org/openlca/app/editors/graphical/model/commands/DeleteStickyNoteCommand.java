package org.openlca.app.editors.graphical.model.commands;

import org.eclipse.gef.commands.Command;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.StickyNote;


public class DeleteStickyNoteCommand extends Command {
	/** Node to remove. */
	private final StickyNote child;
	/** Graph to remove from. */
	private final Graph parent;

	/** True, if child was removed from its parent. */
	private boolean wasRemoved;

	/**
	 * Create a command that will remove the node from its parent.
	 *
	 * @param parent
	 *            the parent containing the child
	 * @param child
	 *            the component to remove
	 * @throws IllegalArgumentException
	 *             if any parameter is null
	 */
	public DeleteStickyNoteCommand(Graph parent, StickyNote child) {
		if (parent == null || child == null) {
			throw new IllegalArgumentException();
		}
		setLabel("delete note");
		this.parent = parent;
		this.child = child;
	}

	@Override
	public boolean canExecute() {
		return !(child == null);
	}

	@Override
	public boolean canUndo() {
		return wasRemoved;
	}

	@Override
	public void execute() {
		redo();
	}

	@Override
	public void redo() {
		wasRemoved = parent.removeChild(child);
		parent.editor.setDirty();
	}

	@Override
	public void undo() {
		parent.addChild(child);
		parent.editor.setDirty();
	}

}
