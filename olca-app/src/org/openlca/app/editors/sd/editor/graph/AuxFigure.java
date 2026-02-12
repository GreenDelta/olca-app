package org.openlca.app.editors.sd.editor.graph;

import org.eclipse.draw2d.Ellipse;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.StackLayout;

class AuxFigure extends Ellipse {

	private final Label label = new Label();

	AuxFigure() {
		setLayoutManager(new StackLayout());
		add(label);
	}

	void setText(String text) {
		label.setText(text);
	}

}
