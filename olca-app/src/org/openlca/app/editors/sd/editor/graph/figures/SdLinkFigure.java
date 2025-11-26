package org.openlca.app.editors.sd.editor.graph.figures;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.PolygonDecoration;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.openlca.app.editors.sd.editor.graph.model.SdLink;
import org.openlca.app.util.Colors;

/**
 * Figure for a connection (link) between nodes in the SD graph.
 * Can represent flow connections (solid lines) or information links (dashed lines).
 */
public class SdLinkFigure extends PolylineConnection {

	private static final Color FLOW_COLOR = Colors.get(70, 130, 180);
	private static final Color INFO_COLOR = Colors.get(150, 150, 150);

	private final SdLink link;
	private boolean selected;

	public SdLinkFigure(SdLink link) {
		this.link = link;
		updateStyle();
	}

	public void updateStyle() {
		if (link.isFlowConnection()) {
			// Flow connections: solid line with arrow
			setForegroundColor(FLOW_COLOR);
			setLineStyle(SWT.LINE_SOLID);
			setLineWidth(2);
		} else {
			// Information links: dashed line with arrow
			setForegroundColor(INFO_COLOR);
			setLineStyle(SWT.LINE_DASH);
			setLineWidth(1);
		}

		// Add arrow decoration at target
		var decoration = new PolygonDecoration();
		decoration.setScale(8, 4);
		setTargetDecoration(decoration);
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
		if (selected) {
			setLineWidth(link.isFlowConnection() ? 3 : 2);
			setForegroundColor(ColorConstants.blue);
		} else {
			updateStyle();
		}
	}

	@Override
	public void paintFigure(Graphics g) {
		if (selected) {
			g.setLineWidth(link.isFlowConnection() ? 3 : 2);
		}
		super.paintFigure(g);
	}
}
