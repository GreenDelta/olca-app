package org.openlca.app.editors.graphical.figures;

import static org.openlca.app.components.graphics.model.Side.INPUT;

import org.eclipse.draw2d.Clickable;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.app.components.graphics.model.Side;
import org.openlca.app.util.Colors;

public class PlusMinusButton extends Clickable {

	public final PlusMinusFigure icon;

	/**
	 * @param side Side.INPUT or Side.OUTPUT.
	 */
	public PlusMinusButton(Node node, Side side) {
		setContents(icon = new PlusMinusFigure(node, side));
		setVisible(side == Side.INPUT
				? node.hasLinkedInput()
				: node.hasLinkedOutput());
		setEnabled(node.isButtonEnabled(side));
	}

	public static class PlusMinusFigure extends Figure {

		public static final Dimension SIZE = new Dimension(14, 14);
		public static int LINE_WIDTH = 1;

		private final Node node;
		private final Side side;

		PlusMinusFigure(Node node, Side side) {
			this.node = node;
			this.side = side;
			var theme = node.getGraph().getEditor().getTheme();
			var box = node.getThemeBox();
			setBackgroundColor(theme.boxBackgroundColor(box));
		}

		public Dimension getPreferredSize(int wHint, int hHint) {
			return SIZE;
		}

		@Override
		public void paintFigure(Graphics g) {
			g.setAntialias(SWT.ON);

			var theme = node.getGraph().getEditor().getTheme();
			var box = node.getThemeBox();

			setEnabled(node.isButtonEnabled(side));
			var isVisible = side == INPUT
					? node.hasLinkedInput()
					: node.hasLinkedOutput();
			setVisible(isVisible);

			// Painting
			if (isEnabled())
				g.setForegroundColor(theme.boxBorderColor(box));
			else
				g.setForegroundColor(Colors.gray());
			g.setLineWidth(LINE_WIDTH);
			Rectangle r = getBounds().getCopy();

			// Adding a marge.
			r.translate(LINE_WIDTH, LINE_WIDTH);
			r.setSize(SIZE.getShrinked(LINE_WIDTH * 2, LINE_WIDTH * 2));

			// Drawing the circle
			g.drawOval(r);

			// Drawing the line(s) inside the circle with some marge.
			r.translate(LINE_WIDTH * 3, LINE_WIDTH * 3);
			r.setSize(SIZE.getShrinked(LINE_WIDTH * 8, LINE_WIDTH * 8));

			g.drawLine(r.getLeft(), r.getRight());
			if (!node.isExpanded(side))
				g.drawLine(r.getTop(), r.getBottom());

			g.restoreState();
			super.paintFigure(g);
		}

	}

}
