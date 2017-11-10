package org.openlca.app.results.analysis.sankey.model;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.swt.SWT;
import org.openlca.app.results.analysis.sankey.SankeyDiagram;

class LinkFigure extends PolylineConnection {

	private final int lineWidth;
	private final SankeyDiagram diagram;

	LinkFigure(int lineWidth, SankeyDiagram diagram) {
		super();
		setAntialias(SWT.ON);
		this.diagram = diagram;
		this.lineWidth = lineWidth;
	}

	@Override
	public void paint(Graphics graphics) {
		setLineWidth((int) (lineWidth * diagram.zoom));
		super.paint(graphics);
	}
}
