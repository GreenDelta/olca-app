package org.openlca.app.editors.graphical.figures;

import org.eclipse.draw2d.Clickable;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.openlca.app.editors.graphical.model.StickyNote;
import org.openlca.app.tools.graphics.themes.Theme;
import org.openlca.app.util.Colors;

public class CloseButton extends Clickable {

	public final CrossFigure icon;

	public CloseButton(StickyNote note) {
		setContents(icon = new CrossFigure(note));
		setVisible(true);
		setEnabled(true);
	}

	public static class CrossFigure extends Figure {

		public static final Dimension SIZE = new Dimension(14, 14);
		public static int LINE_WIDTH = 1;

		CrossFigure(StickyNote note) {
			var theme = note.getGraph().getConfig().getTheme();
			var box = Theme.Box.STICKY_NOTE;
			setBackgroundColor(theme.boxBackgroundColor(box));
		}

		public Dimension getPreferredSize(int wHint, int hHint) {
			return SIZE;
		}

		@Override
		public void paintFigure(Graphics g) {
			g.setAntialias(SWT.ON);

			// Painting
			g.setForegroundColor(Colors.gray());
			g.setLineWidth(LINE_WIDTH);
			Rectangle r = getBounds().getCopy();

			// Adding a marge.
			r.translate(LINE_WIDTH, LINE_WIDTH);
			r.setSize(SIZE.getShrinked(LINE_WIDTH * 2, LINE_WIDTH * 2));

			// Drawing the circle
			g.drawOval(r);

			// Drawing the lines with some marge.
			r.translate(LINE_WIDTH * 3, LINE_WIDTH * 3);
			r.setSize(SIZE.getShrinked(LINE_WIDTH * 8, LINE_WIDTH * 8));

			g.drawLine(r.getTopLeft(), r.getBottomRight());
			g.drawLine(r.getTopRight(), r.getBottomLeft());

			g.restoreState();
			super.paintFigure(g);
		}

	}

}
