package org.openlca.app.editors.graphical.model.commands;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.eclipse.osgi.util.NLS;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.layouts.StickyNoteLayoutInfo;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.StickyNote;

public class CreateStickyNoteCommand extends Command {

	private final GraphEditor editor;
	private final Graph graph;
	private final Rectangle constraint;
	private int index = -1;
	private StickyNote stickyNote;

	public CreateStickyNoteCommand(Graph graph, Rectangle constraint) {
		this.graph = graph;
		this.constraint = constraint;
		this.editor = graph.editor;
		setLabel(NLS.bind(M.Add.toLowerCase(), M.Note));
	}

	public CreateStickyNoteCommand(Graph graph, Rectangle constraint, int index) {
		this(graph, constraint);
		this.index = index;
	}

	public void execute() {
		// Add the sticky note to the graph.
		var location = constraint.getLocation();
		var size = (new Dimension(-1, -1)).equals(constraint.getSize())
			? StickyNote.DEFAULT_SIZE
			: constraint.getSize();
		var info = new StickyNoteLayoutInfo(location, size);
		stickyNote = graph.editor.getGraphFactory().createStickyNote(info);
		StickyNoteDialog.open(stickyNote);

		if (this.index > 0)
			this.graph.addChild(stickyNote, this.index);
		else this.graph.addChild(stickyNote);

		editor.setDirty();
	}

	public void undo() {
		// Remove the note from the graph's children.
		this.graph.removeChild(stickyNote);

		editor.setDirty();
	}

}
