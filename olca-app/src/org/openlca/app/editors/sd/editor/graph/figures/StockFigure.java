package org.openlca.app.editors.sd.editor.graph.figures;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.ToolbarLayout;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.graphics.Color;
import org.openlca.app.editors.sd.editor.graph.model.SdNode;
import org.openlca.app.util.Colors;

/**
 * Figure for a Stock node.
 * Stocks are drawn as rectangles (box shape) representing accumulations.
 */
public class StockFigure extends Figure {

	private static final Color FILL_COLOR = Colors.get(200, 220, 255);
	private static final Color BORDER_COLOR = Colors.get(70, 130, 180);

	private final SdNode node;
	private final Label nameLabel;

	public StockFigure(SdNode node) {
		this.node = node;

		setOpaque(true);
		setBackgroundColor(FILL_COLOR);
		setBorder(new LineBorder(BORDER_COLOR, 2));

		var layout = new ToolbarLayout();
		layout.setMinorAlignment(ToolbarLayout.ALIGN_CENTER);
		layout.setStretchMinorAxis(true);
		layout.setSpacing(2);
		setLayoutManager(layout);

		nameLabel = new Label(node.getDisplayName());
		nameLabel.setForegroundColor(ColorConstants.black);
		add(nameLabel);

		setToolTip(new Label("Stock: " + node.getVariableName()));
	}

	public void updateName() {
		nameLabel.setText(node.getDisplayName());
	}

	@Override
	protected void paintFigure(Graphics g) {
		super.paintFigure(g);
		// Stock is a simple rectangle - already handled by default painting
		// with background color and border
	}

	@Override
	public Rectangle getBounds() {
		return super.getBounds();
	}
}
