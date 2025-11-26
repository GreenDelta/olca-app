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
 * Figure for an Auxiliary variable node.
 * Auxiliaries are drawn as circles representing calculated variables.
 */
public class AuxiliaryFigure extends Figure {

	private static final Color FILL_COLOR = Colors.get(200, 255, 200);
	private static final Color BORDER_COLOR = Colors.get(50, 150, 50);

	private final SdNode node;
	private final Label nameLabel;

	public AuxiliaryFigure(SdNode node) {
		this.node = node;
		setOpaque(false);

		nameLabel = new Label(node.getDisplayName());
		nameLabel.setForegroundColor(ColorConstants.black);
		add(nameLabel);

		setToolTip(new Label("Auxiliary: " + node.getVariableName()));
	}

	public void updateName() {
		nameLabel.setText(node.getDisplayName());
	}

	@Override
	protected void paintFigure(Graphics g) {
		Rectangle r = getBounds().getCopy();

		// Draw ellipse/circle for auxiliary
		g.setBackgroundColor(FILL_COLOR);
		g.setForegroundColor(BORDER_COLOR);
		g.setLineWidth(2);

		// Fill the ellipse
		g.fillOval(r.x + 1, r.y + 1, r.width - 2, r.height - 2);
		// Draw the border
		g.drawOval(r.x + 1, r.y + 1, r.width - 3, r.height - 3);
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
