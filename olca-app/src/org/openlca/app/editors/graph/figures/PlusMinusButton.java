package org.openlca.app.editors.graph.figures;

import org.eclipse.draw2d.*;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.app.editors.graph.model.Node;
import org.openlca.app.editors.graph.themes.Theme;

public class PlusMinusButton extends Clickable {

	public final PlusMinusFigure icon;

	public PlusMinusButton(Node node, Node.Side side) {
		setContents(icon = new PlusMinusFigure(node, side));
		setEnabled(node.shouldExpanderBeVisible(side));
		setVisible(node.shouldExpanderBeVisible(side));
	}

	public static class PlusMinusFigure extends Figure {

		public static final Dimension SIZE = new Dimension(14, 14);
		public static int LINE_WIDTH = 2;

		private final Node node;
		private final Node.Side side;

		PlusMinusFigure(Node node, Node.Side side) {
			this.node = node;
			this.side = side;
			var theme = node.getConfig().getTheme();
			var box = Theme.Box.of(node);
			setBackgroundColor(theme.boxBackgroundColor(box));
		}

		public Dimension getPreferredSize(int wHint, int hHint) {
			return SIZE;
		}

		@Override
		public void paintFigure(Graphics g) {
			var theme = node.getConfig().getTheme();
			var box = Theme.Box.of(node);

			setEnabled(node.shouldExpanderBeVisible(side));
			setVisible(node.shouldExpanderBeVisible(side));

			// Painting
			g.setForegroundColor(theme.boxBorderColor(box));
			g.setLineWidth(LINE_WIDTH);
			Rectangle r = getBounds().getCopy();
			r.translate(LINE_WIDTH, LINE_WIDTH);
			r.setSize(SIZE.getShrinked(LINE_WIDTH * 2, LINE_WIDTH * 2));
			g.drawOval(r);
			g.drawLine(r.getLeft(), r.getRight());
			if (!node.isExpanded(side))
				g.drawLine(r.getTop(), r.getBottom());
			g.setForegroundColor(ColorConstants.red);
			g.restoreState();
			super.paintFigure(g);
		}

	}

}
