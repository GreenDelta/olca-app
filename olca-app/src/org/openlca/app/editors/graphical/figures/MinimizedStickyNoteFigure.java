package org.openlca.app.editors.graphical.figures;

import org.eclipse.draw2d.GridLayout;
import org.openlca.app.editors.graphical.model.StickyNote;
import org.openlca.app.tools.graphics.figures.GridPos;

public class MinimizedStickyNoteFigure extends StickyNoteFigure {

	public MinimizedStickyNoteFigure(StickyNote note) {
		super(note);

		var layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		setLayoutManager(layout);

		var header = new StickyNoteHeader();
		add(header, GridPos.fill());

		setOpaque(true);
	}

}
