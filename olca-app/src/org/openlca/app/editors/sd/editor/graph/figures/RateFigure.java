package org.openlca.app.editors.sd.editor.graph.figures;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.graphics.Color;
import org.openlca.app.editors.sd.editor.graph.model.SdNode;
import org.openlca.app.util.Colors;

/**
 * Figure for a Rate (flow) node.
 * Rates are drawn as a valve symbol (hourglass/bowtie shape) representing
 * the flow rate between stocks.
 */
public class RateFigure extends Figure {

	private static final Color FILL_COLOR = Colors.get(255, 200, 150);
	private static final Color BORDER_COLOR = Colors.get(200, 100, 50);

	private final SdNode node;
	private final Label nameLabel;

	public RateFigure(SdNode node) {
		this.node = node;
		setOpaque(false);

		nameLabel = new Label(node.getDisplayName());
		nameLabel.setForegroundColor(ColorConstants.black);
		add(nameLabel);

		setToolTip(new Label("Rate: " + node.getVariableName()));
	}

	public void updateName() {
		nameLabel.setText(node.getDisplayName());
	}

	@Override
	protected void paintFigure(Graphics g) {
		Rectangle r = getBounds().getCopy();

		// Draw the valve/hourglass shape
		g.setBackgroundColor(FILL_COLOR);
		g.setForegroundColor(BORDER_COLOR);
		g.setLineWidth(2);

		// Calculate points for the hourglass/valve shape
		int midX = r.x + r.width / 2;
		int midY = r.y + r.height / 2;
		int halfWidth = r.width / 2 - 2;
		int halfHeight = r.height / 2 - 2;

		// Draw hourglass shape (two triangles meeting at center)
		int[] points = new int[] {
			r.x + 2, r.y + 2,                    // top-left
			r.x + r.width - 2, r.y + 2,          // top-right
			midX, midY,                           // center
			r.x + r.width - 2, r.y + r.height - 2, // bottom-right
			r.x + 2, r.y + r.height - 2,         // bottom-left
			midX, midY                            // center
		};

		g.fillPolygon(points);
		g.drawPolygon(points);
	}

	@Override
	public void setBounds(Rectangle rect) {
		super.setBounds(rect);
		// Position the name label below the figure
		if (nameLabel != null) {
			var labelSize = nameLabel.getPreferredSize();
			nameLabel.setBounds(new Rectangle(
				rect.x + (rect.width - labelSize.width) / 2,
				rect.y + rect.height + 2,
				labelSize.width,
				labelSize.height
			));
		}
	}
}
