package org.openlca.app.editors.graphical.figures;

import org.eclipse.draw2d.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.openlca.app.editors.graphical.model.StickyNote;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.tools.graphics.figures.ComponentFigure;
import org.openlca.app.tools.graphics.themes.Theme;


public class StickyNoteFigure extends ComponentFigure {

	private final StickyNote note;
	public final CloseButton closeButton;

	public StickyNoteFigure(StickyNote note) {
		super(note);
		this.note = note;

		closeButton = new CloseButton(note);
		setToolTip(new Label(note.title));
	}

	class StickyNoteHeader extends Figure {

		StickyNoteHeader() {
			var theme = note.getGraph().getConfig().getTheme();
			var box = Theme.Box.STICKY_NOTE;

			GridLayout layout = new GridLayout(3, false);
			layout.marginHeight = 2;
			layout.marginWidth = 6;
			layout.horizontalSpacing = 8;
			setLayoutManager(layout);

			add(new ImageFigure(Icon.COMMENT.get()),
					new GridData(SWT.LEAD, SWT.CENTER, false, true));

			var label = new Label(note.title);
			label.setForegroundColor(theme.boxFontColor(box));
			add(label, new GridData(SWT.LEAD, SWT.CENTER, true, true));

			var bodyBackground = theme.boxBackgroundColor(box);
			var darkerBackground = new Color(
					(int) (0.95 * bodyBackground.getRed()),
					(int) (0.95 * bodyBackground.getGreen()),
					(int) (0.95 * bodyBackground.getBlue())
			);

			add(closeButton, new GridData(SWT.TRAIL, SWT.CENTER, false, true));

			setBackgroundColor(darkerBackground);
			setOpaque(true);
		}

	}

	public String toString() {
		return "Figure of " + note;
	}

}
