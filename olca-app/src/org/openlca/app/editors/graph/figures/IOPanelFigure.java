package org.openlca.app.editors.graph.figures;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.draw2d.ScrollPane;

public class IOPanelFigure extends Figure {

	private ScrollPane scrollpane;

	public IOPanelFigure() {
		scrollpane = new ScrollPane();

		var layout = new GridLayout(1, true);
		layout.horizontalSpacing = 4;
		layout.verticalSpacing = 4;
		layout.marginHeight = 5;
		layout.marginWidth = 0;
		setLayoutManager(layout);

	}
}
