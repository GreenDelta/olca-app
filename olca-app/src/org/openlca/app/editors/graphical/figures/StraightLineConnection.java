package org.openlca.app.editors.graphical.figures;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.PolygonDecoration;
import org.eclipse.draw2d.PolylineConnection;
import org.openlca.app.editors.graphical.model.GraphLink;

import static org.eclipse.swt.SWT.ON;

public class StraightLineConnection extends PolylineConnection {

	private final GraphLink link;

	public StraightLineConnection(GraphLink link) {
		this.link = link;
		setTargetDecoration(new PolygonDecoration());
		setLineWidth(1);
	}

	@Override
	public void paint(Graphics g) {
		setAntialias(ON);
		var provider = link.provider();
		var theme = provider != null
				? provider.getGraph().getEditor().config.getTheme()
				: null;
		if (theme != null) {
			setForegroundColor(theme.linkColor());
		} else {
			setForegroundColor(ColorConstants.black);
		}
		super.paint(g);
	}

}
