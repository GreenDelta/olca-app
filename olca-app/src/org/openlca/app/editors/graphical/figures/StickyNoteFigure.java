package org.openlca.app.editors.graphical.figures;

import org.eclipse.draw2d.*;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.swt.SWT;
import org.openlca.app.editors.graphical.model.StickyNote;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.tools.graphics.figures.ComponentFigure;
import org.openlca.app.tools.graphics.themes.Theme;


public class StickyNoteFigure extends ComponentFigure {

	public final static Dimension HEADER_ARC_SIZE = new Dimension(15, 15);
	private final StickyNote note;

	public StickyNoteFigure(StickyNote note) {
		super(note);
		this.note = note;

		setToolTip(new Label(note.title));
	}

	class StickyNoteHeader extends Figure {

		StickyNoteHeader() {
			var theme = note.getGraph().getConfig().getTheme();
			var box = Theme.Box.STICKY_NOTE;

			GridLayout layout = new GridLayout(2, false);
			layout.marginHeight = 2;
			layout.marginWidth = 6;
			layout.horizontalSpacing = 8;
			setLayoutManager(layout);

			add(new ImageFigure(Icon.COMMENT.get()),
					new GridData(SWT.LEAD, SWT.CENTER, false, true));

			var label = new Label(note.title);
			label.setForegroundColor(theme.boxFontColor(box));
			add(label, new GridData(SWT.LEAD, SWT.CENTER, true, true));

			setBackgroundColor(theme.boxBackgroundColor(box));
			setOpaque(true);
		}

	}

	public String toString() {
		return "Figure of " + note;
	}

}
