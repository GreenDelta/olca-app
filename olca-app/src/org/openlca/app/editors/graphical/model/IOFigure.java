package org.openlca.app.editors.graphical.model;

import java.util.List;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.GridLayout;

class IOFigure extends Figure {

	IOFigure() {
		var layout = new GridLayout(2, true);
		layout.horizontalSpacing = 4;
		layout.verticalSpacing = 0;
		layout.marginHeight = 4;
		layout.marginWidth = 0;
		setLayoutManager(layout);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<ExchangeFigure> getChildren() {
		return super.getChildren();
	}

}
